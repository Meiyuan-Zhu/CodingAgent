<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import ChatTimeline from './components/ChatTimeline.vue'
import ComposerBox from './components/ComposerBox.vue'
import InspectorPane from './components/InspectorPane.vue'
import ProjectSidebar from './components/ProjectSidebar.vue'
import { fetchHealth, type HealthResponse } from './api/health'
import { approveToolCall, cancelRun, createRun, fetchRun, fetchRunEvents, rejectToolCall, type RunEvent, type RunResponse } from './api/runs'
import { buildToolCards } from './run/toolCards'
import { buildTimelineItems, pendingApprovalView, type InspectorSelection } from './run/timeline'

const workspacePath = '/Users/zhumeiyuan/Desktop/CodingAgent'
const defaultPrompt = 'Fix the failing Python pricing tests in the demo workspace, then run the unittest command to verify the fix'
const terminalStatuses = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED'])

const health = ref<HealthResponse | null>(null)
const loadingHealth = ref(true)
const healthError = ref<string | null>(null)
const taskDraft = ref(defaultPrompt)
const submitting = ref(false)
const cancelling = ref(false)
const resolvingApproval = ref(false)
const runError = ref<string | null>(null)
const activeRun = ref<RunResponse | null>(null)
const activePrompt = ref('')
const runHistory = ref<RunResponse[]>([])
const events = ref<RunEvent[]>([])
const eventSource = ref<EventSource | null>(null)
const inspectorSelection = ref<InspectorSelection>({ kind: 'welcome' })
const inspectorOpen = ref(true)
const inspectorWidth = ref(420)
const resizingPanel = ref<'inspector' | null>(null)

const layoutStyle = computed(() => ({
  gridTemplateColumns: `300px minmax(520px, 1fr) ${inspectorOpen.value ? `${inspectorWidth.value}px` : '0px'}`,
}))

const healthStatus = computed(() => {
  if (loadingHealth.value) return 'checking'
  if (healthError.value) return 'offline'
  return health.value?.status ?? 'unknown'
})

const canSubmit = computed(() => {
  return !submitting.value && !loadingHealth.value && !healthError.value && taskDraft.value.trim().length > 0
})

const canCancel = computed(() => {
  return !cancelling.value && !!activeRun.value && !terminalStatuses.has(activeRun.value.status)
})

const toolCards = computed(() => buildToolCards(events.value))
const timelineItems = computed(() => buildTimelineItems(events.value, activePrompt.value, activeRun.value))
const pendingApproval = computed(() => pendingApprovalView(toolCards.value))

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

onMounted(async () => {
  window.addEventListener('mousemove', handlePanelResize)
  window.addEventListener('mouseup', stopPanelResize)

  try {
    health.value = await fetchHealth()
  } catch (caught) {
    healthError.value = caught instanceof Error ? caught.message : 'Unknown backend error'
  } finally {
    loadingHealth.value = false
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
  try {
    const run = await createRun(prompt)
    activeRun.value = run
    activePrompt.value = prompt
    upsertRun(run)
    connectEventStream(run.id)
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : 'Failed to create run'
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
    runError.value = caught instanceof Error ? caught.message : 'Failed to cancel run'
  } finally {
    cancelling.value = false
  }
}

async function approvePendingTool() {
  if (!activeRun.value || !pendingApproval.value || resolvingApproval.value) return
  resolvingApproval.value = true
  runError.value = null
  try {
    const run = await approveToolCall(activeRun.value.id, pendingApproval.value.toolCallId)
    activeRun.value = run
    upsertRun(run)
    inspectorSelection.value = pendingApproval.value.name === 'run_command'
      ? { kind: 'command', toolCallId: pendingApproval.value.toolCallId }
      : { kind: 'diff', toolCallId: pendingApproval.value.toolCallId }
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : 'Failed to approve tool call'
  } finally {
    resolvingApproval.value = false
  }
}

async function rejectPendingTool() {
  if (!activeRun.value || !pendingApproval.value || resolvingApproval.value) return
  resolvingApproval.value = true
  runError.value = null
  try {
    const run = await rejectToolCall(activeRun.value.id, pendingApproval.value.toolCallId)
    activeRun.value = run
    upsertRun(run)
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : 'Failed to reject tool call'
  } finally {
    resolvingApproval.value = false
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
  if (card.name === 'run_command') {
    inspectorSelection.value = { kind: 'command', toolCallId }
  } else if (card.result && typeof card.result.unifiedDiff === 'string') {
    inspectorSelection.value = { kind: 'diff', toolCallId }
  } else {
    inspectorSelection.value = { kind: 'tool', toolCallId }
  }
}

async function selectRun(run: RunResponse) {
  closeEventStream()
  activeRun.value = run
  activePrompt.value = ''
  runError.value = null
  inspectorSelection.value = { kind: 'welcome' }
  upsertRun(run)
  try {
    events.value = await fetchRunEvents(run.id)
  } catch (caught) {
    events.value = []
    runError.value = caught instanceof Error ? caught.message : 'Failed to load run events'
  }
}

function resetComposer() {
  activeRun.value = null
  activePrompt.value = ''
  events.value = []
  runError.value = null
  taskDraft.value = defaultPrompt
  inspectorSelection.value = { kind: 'welcome' }
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
    'model_message_received',
    'tool_call_requested',
    'tool_call_started',
    'tool_call_finished',
    'run_finished',
  ]

  for (const eventType of eventTypes) {
    source.addEventListener(eventType, (message) => {
      const event = JSON.parse((message as MessageEvent).data) as RunEvent
      upsertEvent(event)
      reflectRunStatus(event)
      if (event.type === 'RUN_FINISHED') {
        refreshRun(runId)
        source.close()
      }
    })
  }

  source.onerror = () => {
    source.close()
    if (!activeRun.value?.status || !terminalStatuses.has(activeRun.value.status)) {
      runError.value = 'Event stream closed before the run reached a terminal state.'
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
      :workspace-path="workspacePath"
      @new-task="resetComposer"
      @select-run="selectRun"
    />

    <section class="workspace-column" aria-label="Conversation workspace">
      <header class="workspace-header">
        <div>
          <p class="section-label">Local workspace</p>
          <h1>{{ workspacePath }}</h1>
        </div>
        <div class="run-health">
          <span class="health-pill" :class="`health-${healthStatus}`">{{ healthStatus }}</span>
          <span v-if="activeRun" class="run-chip">{{ activeRun.status.toLowerCase() }}</span>
        </div>
      </header>

      <ChatTimeline
        :items="timelineItems"
        :pending-approval="pendingApproval"
        :selected-tool-call-id="inspectorSelection.kind === 'tool' || inspectorSelection.kind === 'diff' || inspectorSelection.kind === 'command' ? inspectorSelection.toolCallId : null"
        :resolving-approval="resolvingApproval"
        :run-error="runError"
        @select-tool="selectTool"
        @approve="approvePendingTool"
        @reject="rejectPendingTool"
      />

      <ComposerBox
        v-model="taskDraft"
        :disabled="!canSubmit"
        :submitting="submitting"
        :cancelling="cancelling"
        :can-cancel="canCancel"
        @submit="submitRun"
        @cancel="cancelActiveRun"
      />
    </section>

    <button v-if="!inspectorOpen" class="inspector-reopen" type="button" aria-label="Show inspector" @click="inspectorOpen = true">Inspector</button>
    <div v-if="inspectorOpen" class="inspector-resize-handle" role="separator" aria-orientation="vertical" title="Drag to resize inspector" @mousedown.prevent="startInspectorResize"></div>

    <InspectorPane
      :open="inspectorOpen"
      :selection="inspectorSelection"
      :events="events"
      :tool-cards="toolCards"
      :active-run="activeRun"
      :health-status="healthStatus"
      @select="inspectorSelection = $event"
      @toggle="inspectorOpen = !inspectorOpen"
    />

  </main>
</template>
