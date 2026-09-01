<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import ChatTimeline from './components/ChatTimeline.vue'
import ComposerBox from './components/ComposerBox.vue'
import InspectorPane from './components/InspectorPane.vue'
import ProjectSidebar from './components/ProjectSidebar.vue'
import UiIcon from './components/UiIcon.vue'
import { fetchHealth, type HealthResponse } from './api/health'
import { approveToolCall, cancelRun, createRun, fetchRun, fetchRunEvents, fetchRuns, rejectToolCall, undoWorkspaceChange, type RunEvent, type RunResponse } from './api/runs'
import { fetchWorkspaceFile, fetchWorkspaceFiles, type WorkspaceFileEntry, type WorkspaceFileResponse } from './api/workspace'
import { buildToolCards } from './run/toolCards'
import { buildTimelineItems, pendingApprovalView, type InspectorSelection } from './run/timeline'

const workspacePath = '/Users/zhumeiyuan/Desktop/CodingAgent'
const defaultPrompt = ''
const terminalStatuses = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED'])

const health = ref<HealthResponse | null>(null)
const loadingHealth = ref(true)
const healthError = ref<string | null>(null)
const taskDraft = ref(defaultPrompt)
const submitting = ref(false)
const cancelling = ref(false)
const resolvingApproval = ref(false)
const autoApprove = ref(false)
const undoingToolCallId = ref<string | null>(null)
const runError = ref<string | null>(null)
const activeRun = ref<RunResponse | null>(null)
const activePrompt = ref('')
const runHistory = ref<RunResponse[]>([])
const runTitles = ref<Record<string, string>>({})
const events = ref<RunEvent[]>([])
const eventSource = ref<EventSource | null>(null)
const inspectorSelection = ref<InspectorSelection>({ kind: 'welcome' })
const inspectorOpen = ref(true)
const inspectorWidth = ref(420)
const resizingPanel = ref<'inspector' | null>(null)
const workspaceEntriesByDirectory = ref<Record<string, WorkspaceFileEntry[]>>({})
const expandedDirectories = ref<string[]>([])
const selectedWorkspaceFile = ref<WorkspaceFileResponse | null>(null)
const loadingWorkspacePath = ref<string | null>(null)
const workspaceError = ref<string | null>(null)

const layoutStyle = computed(() => ({
  gridTemplateColumns: `288px minmax(560px, 1fr) ${inspectorOpen.value ? `${inspectorWidth.value}px` : '0px'}`,
}))

const canSubmit = computed(() => {
  return !submitting.value && !loadingHealth.value && !healthError.value && taskDraft.value.trim().length > 0
})

const canCancel = computed(() => {
  return !cancelling.value && !!activeRun.value && !terminalStatuses.has(effectiveRunStatus.value)
})

const toolCards = computed(() => buildToolCards(events.value))
const latestFinishedEvent = computed(() => [...events.value].reverse().find((event) => event.type === 'RUN_FINISHED') ?? null)
const effectiveRunStatus = computed(() => {
  const finishedStatus = latestFinishedEvent.value?.payload.status
  return typeof finishedStatus === 'string' ? finishedStatus : activeRun.value?.status ?? ''
})
const effectiveActiveRun = computed(() => {
  if (!activeRun.value) return null
  return {
    ...activeRun.value,
    status: effectiveRunStatus.value || activeRun.value.status,
    stopReason: typeof latestFinishedEvent.value?.payload.stopReason === 'string'
      ? latestFinishedEvent.value.payload.stopReason
      : activeRun.value.stopReason,
  }
})
const timelineItems = computed(() => buildTimelineItems(events.value, activePrompt.value, effectiveActiveRun.value))
const pendingApproval = computed(() => pendingApprovalView(toolCards.value))
const workspaceTitle = computed(() => activeRun.value ? runTitles.value[activeRun.value.id] ?? '代码任务' : 'CodingAgent')
const workspaceSubtitle = computed(() => activeRun.value ? workspacePath : '本地 workspace')

watch(inspectorSelection, (next) => {
  if (next.kind !== 'welcome') inspectorOpen.value = true
})

watch(toolCards, (cards) => {
  const current = inspectorSelection.value
  const selectedToolExists = current.kind === 'tool' || current.kind === 'diff' || current.kind === 'command'
    ? cards.some((card) => card.id === current.toolCallId)
    : true
  if (!selectedToolExists) {
    inspectorSelection.value = { kind: 'welcome' }
  }
}, { deep: true })

watch(pendingApproval, (approval) => {
  if (!approval || !autoApprove.value || resolvingApproval.value) return
  void approvePendingTool()
})

watch(autoApprove, (enabled) => {
  if (!enabled || !pendingApproval.value || resolvingApproval.value) return
  void approvePendingTool()
})

onMounted(async () => {
  window.addEventListener('mousemove', handlePanelResize)
  window.addEventListener('mouseup', stopPanelResize)

  try {
    health.value = await fetchHealth()
  } catch (caught) {
    healthError.value = caught instanceof Error ? caught.message : '后端状态未知'
  } finally {
    loadingHealth.value = false
  }

  if (!healthError.value) {
    try {
      await loadRunHistory()
      await ensureWorkspaceDirectory('.')
    } catch (caught) {
      runError.value = caught instanceof Error ? caught.message : '加载历史任务失败'
    }
  }
})

onUnmounted(() => {
  closeEventStream()
  window.removeEventListener('mousemove', handlePanelResize)
  window.removeEventListener('mouseup', stopPanelResize)
})

async function submitRun() {
  if (!canSubmit.value) return
  submitting.value = true
  runError.value = null
  events.value = []
  inspectorSelection.value = { kind: 'welcome' }
  closeEventStream()

  const prompt = taskDraft.value.trim()
  taskDraft.value = ''
  try {
    const run = await createRun(prompt)
    activeRun.value = run
    activePrompt.value = prompt
    runTitles.value = { ...runTitles.value, [run.id]: titleFromPrompt(prompt) }
    upsertRun(run)
    connectEventStream(run.id)
  } catch (caught) {
    taskDraft.value = prompt
    runError.value = caught instanceof Error ? caught.message : '任务创建失败'
  } finally {
    submitting.value = false
  }
}

async function cancelActiveRun() {
  if (!canCancel.value || !activeRun.value) return
  cancelling.value = true
  runError.value = null
  try {
    const run = await cancelRun(activeRun.value.id)
    activeRun.value = run
    upsertRun(run)
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : '任务停止失败'
  } finally {
    cancelling.value = false
  }
}

async function approvePendingTool() {
  if (!activeRun.value || !pendingApproval.value || resolvingApproval.value) return
  const approval = pendingApproval.value
  resolvingApproval.value = true
  runError.value = null
  try {
    const run = await approveToolCall(activeRun.value.id, approval.toolCallId)
    activeRun.value = run
    upsertRun(run)
    if (isFileChangeToolName(approval.name)) {
      inspectorSelection.value = { kind: 'diff', toolCallId: approval.toolCallId }
    } else {
      preserveReviewSelection()
    }
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : 'Failed to approve tool call'
  } finally {
    resolvingApproval.value = false
  }
}

async function rejectPendingTool() {
  if (!activeRun.value || !pendingApproval.value || resolvingApproval.value) return
  const approval = pendingApproval.value
  resolvingApproval.value = true
  runError.value = null
  try {
    const run = await rejectToolCall(activeRun.value.id, approval.toolCallId)
    activeRun.value = run
    upsertRun(run)
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : 'Failed to reject tool call'
  } finally {
    resolvingApproval.value = false
  }
}

async function undoChange(toolCallId: string) {
  if (!activeRun.value || undoingToolCallId.value) return
  undoingToolCallId.value = toolCallId
  runError.value = null
  try {
    await undoWorkspaceChange(activeRun.value.id, toolCallId)
    events.value = await fetchRunEvents(activeRun.value.id)
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : '撤销变更失败'
  } finally {
    undoingToolCallId.value = null
  }
}

function startInspectorResize() {
  if (!inspectorOpen.value) inspectorOpen.value = true
  resizingPanel.value = 'inspector'
}

function handlePanelResize(event: MouseEvent) {
  if (!resizingPanel.value) return
  if (resizingPanel.value === 'inspector') {
    const nextWidth = window.innerWidth - event.clientX
    inspectorWidth.value = clamp(nextWidth, 320, Math.min(720, window.innerWidth - 760))
    return
  }
}

function stopPanelResize() {
  resizingPanel.value = null
}

function clamp(value: number, min: number, max: number) {
  return Math.min(Math.max(value, min), Math.max(min, max))
}

function selectTool(toolCallId: string) {
  const card = toolCards.value.find((item) => item.id === toolCallId)
  if (!card) return
  if (card.result && typeof card.result.unifiedDiff === 'string') {
    inspectorSelection.value = { kind: 'diff', toolCallId }
  } else {
    preserveReviewSelection()
  }
}

function preserveReviewSelection() {
  if (inspectorSelection.value.kind === 'tool' || inspectorSelection.value.kind === 'command') {
    inspectorSelection.value = { kind: 'review' }
  }
}

function isFileChangeToolName(name: string) {
  return name === 'write_file' || name === 'replace_text' || name === 'edit_file'
}

async function selectRun(run: RunResponse) {
  closeEventStream()
  activeRun.value = run
  activePrompt.value = ''
  runError.value = null
  inspectorSelection.value = { kind: 'welcome' }
  selectedWorkspaceFile.value = null
  upsertRun(run)
  try {
    events.value = await fetchRunEvents(run.id)
    await ensureWorkspaceDirectory('.')
    const title = titleFromEvents(events.value)
    if (title) runTitles.value = { ...runTitles.value, [run.id]: title }
  } catch (caught) {
    events.value = []
    runError.value = caught instanceof Error ? caught.message : '加载任务事件失败'
  }
}

async function selectInspector(selection: InspectorSelection) {
  inspectorSelection.value = selection
  if (selection.kind === 'welcome') {
    await ensureWorkspaceDirectory('.')
  }
}

async function toggleWorkspaceDirectory(path: string) {
  const normalized = normalizeDirectory(path)
  if (!workspaceEntriesByDirectory.value[normalized]) {
    await ensureWorkspaceDirectory(normalized)
  }
  const next = new Set(expandedDirectories.value)
  if (next.has(normalized)) {
    next.delete(normalized)
  } else {
    next.add(normalized)
  }
  expandedDirectories.value = [...next]
}

async function openWorkspaceFile(path: string) {
  inspectorSelection.value = { kind: 'file', path }
  selectedWorkspaceFile.value = null
  loadingWorkspacePath.value = path
  workspaceError.value = null
  try {
    selectedWorkspaceFile.value = await fetchWorkspaceFile(path)
  } catch (caught) {
    workspaceError.value = caught instanceof Error ? caught.message : '读取文件失败'
  } finally {
    loadingWorkspacePath.value = null
  }
}

async function ensureWorkspaceDirectory(path: string) {
  const normalized = normalizeDirectory(path)
  if (workspaceEntriesByDirectory.value[normalized]) return
  workspaceError.value = null
  try {
    const listing = await fetchWorkspaceFiles(normalized)
    workspaceEntriesByDirectory.value = {
      ...workspaceEntriesByDirectory.value,
      [normalized]: listing.files,
    }
  } catch (caught) {
    workspaceError.value = caught instanceof Error ? caught.message : '加载 workspace 文件失败'
  }
}

function normalizeDirectory(path: string) {
  return path === '' || path === '.' ? '.' : path.replace(/\/+$/, '')
}

async function loadRunHistory() {
  const runs = await fetchRuns()
  runHistory.value = runs
  for (const run of runs.slice(0, 20)) {
    try {
      const runEvents = await fetchRunEvents(run.id)
      const title = titleFromEvents(runEvents)
      if (title) runTitles.value = { ...runTitles.value, [run.id]: title }
    } catch {
      // Keep the run row visible even if one history title cannot be reconstructed.
    }
  }
}

function resetComposer() {
  activeRun.value = null
  activePrompt.value = ''
  events.value = []
  runError.value = null
  taskDraft.value = defaultPrompt
  inspectorSelection.value = { kind: 'welcome' }
  void ensureWorkspaceDirectory('.')
  closeEventStream()
}

function connectEventStream(runId: string) {
  const source = new EventSource(`/api/runs/${encodeURIComponent(runId)}/events/stream`)
  eventSource.value = source
  const eventTypes = [
    'run_created',
    'user_message_accepted',
    'run_started',
    'run_cancelling',
    'approval_required',
    'approval_resolved',
    'model_requested',
    'model_message_delta',
    'model_message_received',
    'tool_call_requested',
    'tool_call_started',
    'tool_call_finished',
    'change_undone',
    'run_finished',
  ]

  for (const eventType of eventTypes) {
    source.addEventListener(eventType, (message) => {
      const event = JSON.parse((message as MessageEvent).data) as RunEvent
      upsertEvent(event)
      reflectRunStatus(event)
      if (event.type === 'RUN_FINISHED') {
        if (event.payload.status === 'SUCCEEDED') runError.value = null
        refreshRun(runId)
        source.close()
      }
    })
  }

  source.onerror = () => {
    source.close()
    if (!effectiveRunStatus.value || !terminalStatuses.has(effectiveRunStatus.value)) {
      runError.value = '事件流提前断开，正在刷新任务状态。'
      refreshRun(runId)
    }
  }
}

function upsertEvent(event: RunEvent) {
  const index = events.value.findIndex((item) => item.sequence === event.sequence)
  if (index >= 0) {
    events.value.splice(index, 1, event)
  } else {
    events.value.push(event)
    events.value.sort((left, right) => left.sequence - right.sequence)
  }
}


function reflectRunStatus(event: RunEvent) {
  if (!activeRun.value) return
  const statusByEvent: Record<string, string | undefined> = {
    RUN_STARTED: 'RUNNING',
    RUN_CANCELLING: 'CANCELLING',
    APPROVAL_REQUIRED: 'WAITING_FOR_APPROVAL',
    APPROVAL_RESOLVED: 'RUNNING',
  }
  const nextStatus = event.type === 'RUN_FINISHED'
    ? typeof event.payload.status === 'string' ? event.payload.status : activeRun.value.status
    : statusByEvent[event.type]
  if (!nextStatus || activeRun.value.status === nextStatus) return
  const updated = {
    ...activeRun.value,
    status: nextStatus,
    stopReason: event.type === 'RUN_FINISHED' && typeof event.payload.stopReason === 'string'
      ? event.payload.stopReason
      : activeRun.value.stopReason,
    updatedAt: event.occurredAt,
  }
  activeRun.value = updated
  upsertRun(updated)
}

async function refreshRun(runId: string) {
  try {
    const run = await fetchRun(runId)
    activeRun.value = run
    upsertRun(run)
  } catch {
    // Keep the visible conversation if status refresh fails.
  }
}

function upsertRun(run: RunResponse) {
  runHistory.value = [run, ...runHistory.value.filter((item) => item.id !== run.id)]
}

function titleFromEvents(runEvents: RunEvent[]) {
  const event = runEvents.find((item) => item.type === 'USER_MESSAGE_ACCEPTED')
  return typeof event?.payload.prompt === 'string' ? titleFromPrompt(event.payload.prompt) : ''
}

function titleFromPrompt(prompt: string) {
  const normalized = prompt.replace(/\s+/g, ' ').trim()
  return normalized.length > 34 ? `${normalized.slice(0, 34)}…` : normalized
}

function closeEventStream() {
  eventSource.value?.close()
  eventSource.value = null
}
</script>

<template>
  <main class="codex-workbench" :class="{ 'is-resizing': resizingPanel !== null, 'inspector-is-closed': !inspectorOpen }" :style="layoutStyle">
    <ProjectSidebar
      :active-run-id="activeRun?.id ?? null"
      :runs="runHistory"
      :run-titles="runTitles"
      @new-task="resetComposer"
      @select-run="selectRun"
    />

    <section class="workspace-column" aria-label="对话工作区">
      <header class="workspace-header">
        <div class="workspace-titlebar">
          <span class="workspace-folder" aria-hidden="true">
            <UiIcon name="folder" />
          </span>
          <div>
            <h1>{{ workspaceTitle }}</h1>
            <p class="workspace-subtitle">{{ workspaceSubtitle }}</p>
          </div>
        </div>
        <button class="workspace-panel-button" type="button" :aria-label="inspectorOpen ? '收起审查面板' : '展开审查面板'" @click="inspectorOpen = !inspectorOpen">
          <UiIcon :name="inspectorOpen ? 'chevron-right' : 'review'" />
        </button>
      </header>

      <ChatTimeline
        :items="timelineItems"
        :pending-approval="pendingApproval"
        :selected-tool-call-id="inspectorSelection.kind === 'tool' || inspectorSelection.kind === 'diff' || inspectorSelection.kind === 'command' ? inspectorSelection.toolCallId : null"
        :resolving-approval="resolvingApproval"
        :undoing-tool-call-id="undoingToolCallId"
        :run-error="runError"
        @select-tool="selectTool"
        @undo-change="undoChange"
        @approve="approvePendingTool"
        @reject="rejectPendingTool"
      />

      <ComposerBox
        v-model="taskDraft"
        :disabled="!canSubmit"
        :submitting="submitting"
        :cancelling="cancelling"
        :can-cancel="canCancel"
        :auto-approve="autoApprove"
        @submit="submitRun"
        @cancel="cancelActiveRun"
        @set-auto-approve="autoApprove = $event"
      />
    </section>

    <button v-if="!inspectorOpen" class="inspector-reopen" type="button" aria-label="展开审查面板" @click="inspectorOpen = true">
      <UiIcon name="review" />
    </button>
    <div v-if="inspectorOpen" class="inspector-resize-handle" role="separator" aria-orientation="vertical" title="拖拽调整审查面板宽度" @mousedown.prevent="startInspectorResize"></div>

    <InspectorPane
      :open="inspectorOpen"
      :selection="inspectorSelection"
      :events="events"
      :tool-cards="toolCards"
      :undoing-tool-call-id="undoingToolCallId"
      :workspace-entries-by-directory="workspaceEntriesByDirectory"
      :expanded-directories="expandedDirectories"
      :selected-workspace-file="selectedWorkspaceFile"
      :loading-workspace-path="loadingWorkspacePath"
      :workspace-error="workspaceError"
      @select="selectInspector"
      @toggle-directory="toggleWorkspaceDirectory"
      @open-file="openWorkspaceFile"
      @undo-change="undoChange"
      @toggle="inspectorOpen = !inspectorOpen"
    />

  </main>
</template>
