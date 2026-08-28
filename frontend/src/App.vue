<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { fetchHealth, type HealthResponse } from './api/health'
import { approveToolCall, cancelRun, createRun, fetchRun, rejectToolCall, type RunEvent, type RunResponse } from './api/runs'

const health = ref<HealthResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const detailTab = ref<'files' | 'diff' | 'checks'>('files')
const taskDraft = ref('List files in the demo workspace')
const submitting = ref(false)
const cancelling = ref(false)
const resolvingApproval = ref(false)
const runError = ref<string | null>(null)
const activeRun = ref<RunResponse | null>(null)
const activePrompt = ref('')
const runHistory = ref<RunResponse[]>([])
const events = ref<RunEvent[]>([])
const eventSource = ref<EventSource | null>(null)

const files = computed(() => {
  for (const event of [...events.value].reverse()) {
    if (event.type !== 'TOOL_CALL_FINISHED') continue
    if (event.payload.name !== 'list_files') continue
    const content = typeof event.payload.content === 'string' ? event.payload.content : ''
    try {
      const parsed = JSON.parse(content) as { files?: Array<{ path?: string }> }
      const paths = parsed.files?.map((file) => file.path).filter(Boolean) as string[] | undefined
      if (paths?.length) return paths
    } catch {
      return []
    }
  }
  return ['workspaces/demo/README.md', 'workspaces/demo/src/hello.txt']
})


const diffs = computed(() => {
  const parsedDiffs: Array<{ path: string; diff: string }> = []
  for (const event of events.value) {
    if (event.type !== 'TOOL_CALL_FINISHED') continue
    const content = typeof event.payload.content === 'string' ? event.payload.content : ''
    if (!content) continue
    try {
      const parsed = JSON.parse(content) as { path?: string; unifiedDiff?: string }
      if (parsed.unifiedDiff) {
        parsedDiffs.push({ path: parsed.path ?? String(event.payload.name ?? 'workspace change'), diff: parsed.unifiedDiff })
      }
    } catch {
      // Non-JSON tool output is still shown in the event timeline.
    }
  }
  return parsedDiffs
})

const checks = computed(() => [
  { name: 'Backend mock runner', result: 'wired' },
  { name: 'Tool registry', result: 'wired' },
  { name: 'SSE run events', result: activeRun.value ? activeRun.value.status.toLowerCase() : 'ready' },
  { name: 'Approval flow', result: pendingApproval.value ? 'waiting' : 'ready' },
])

const statusLabel = computed(() => {
  if (loading.value) return 'checking'
  if (error.value) return 'offline'
  return health.value?.status ?? 'unknown'
})

const terminalStatuses = ['SUCCEEDED', 'FAILED', 'CANCELLED']

const pendingApproval = computed(() => {
  const resolvedIds = new Set(
    events.value
      .filter((event) => event.type === 'APPROVAL_RESOLVED')
      .map((event) => String(event.payload.toolCallId ?? '')),
  )

  for (const event of [...events.value].reverse()) {
    if (event.type !== 'APPROVAL_REQUIRED') continue
    const toolCallId = String(event.payload.toolCallId ?? '')
    if (!toolCallId || resolvedIds.has(toolCallId)) continue
    return {
      toolCallId,
      name: String(event.payload.name ?? 'tool'),
      arguments: event.payload.arguments,
      reason: approvalReason(event),
    }
  }
  return null
})

const runButtonDisabled = computed(() => {
  return submitting.value || loading.value || !!error.value || taskDraft.value.trim().length === 0
})

const cancelButtonDisabled = computed(() => {
  return cancelling.value || !activeRun.value || terminalStatuses.includes(activeRun.value.status)
})

const approvalButtonDisabled = computed(() => {
  return resolvingApproval.value || !activeRun.value || activeRun.value.status !== 'WAITING_FOR_APPROVAL' || !pendingApproval.value
})

onMounted(async () => {
  try {
    health.value = await fetchHealth()
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : 'Unknown error'
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  eventSource.value?.close()
})

async function submitRun() {
  if (runButtonDisabled.value) return
  submitting.value = true
  runError.value = null
  events.value = []
  eventSource.value?.close()
  const prompt = taskDraft.value.trim()

  try {
    const run = await createRun(prompt)
    activeRun.value = run
    activePrompt.value = prompt
    runHistory.value = [run, ...runHistory.value.filter((item) => item.id !== run.id)]
    connectEventStream(run.id)
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : 'Failed to create run'
  } finally {
    submitting.value = false
  }
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
      if (event.type === 'RUN_FINISHED') {
        refreshRun(runId)
        source.close()
      }
    })
  }

  source.onerror = () => {
    source.close()
    if (!activeRun.value?.status || !terminalStatuses.includes(activeRun.value.status)) {
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
    events.value.sort((a, b) => a.sequence - b.sequence)
  }
}

async function approvePendingTool() {
  if (approvalButtonDisabled.value || !activeRun.value || !pendingApproval.value) return
  resolvingApproval.value = true
  runError.value = null
  try {
    const run = await approveToolCall(activeRun.value.id, pendingApproval.value.toolCallId)
    activeRun.value = run
    runHistory.value = [run, ...runHistory.value.filter((item) => item.id !== run.id)]
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : 'Failed to approve tool call'
  } finally {
    resolvingApproval.value = false
  }
}

async function rejectPendingTool() {
  if (approvalButtonDisabled.value || !activeRun.value || !pendingApproval.value) return
  resolvingApproval.value = true
  runError.value = null
  try {
    const run = await rejectToolCall(activeRun.value.id, pendingApproval.value.toolCallId)
    activeRun.value = run
    runHistory.value = [run, ...runHistory.value.filter((item) => item.id !== run.id)]
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : 'Failed to reject tool call'
  } finally {
    resolvingApproval.value = false
  }
}

async function cancelActiveRun() {
  if (cancelButtonDisabled.value || !activeRun.value) return
  cancelling.value = true
  runError.value = null
  try {
    const run = await cancelRun(activeRun.value.id)
    activeRun.value = run
    runHistory.value = [run, ...runHistory.value.filter((item) => item.id !== run.id)]
  } catch (caught) {
    runError.value = caught instanceof Error ? caught.message : 'Failed to cancel run'
  } finally {
    cancelling.value = false
  }
}

async function refreshRun(runId: string) {
  try {
    const run = await fetchRun(runId)
    activeRun.value = run
    runHistory.value = [run, ...runHistory.value.filter((item) => item.id !== run.id)]
  } catch {
    // Keep the already displayed events if a status refresh fails.
  }
}

function eventKind(type: string) {
  if (type.startsWith('TOOL')) return 'tool'
  if (type.startsWith('APPROVAL')) return 'approval'
  if (type.startsWith('MODEL')) return 'model'
  if (type.startsWith('RUN')) return 'run'
  return 'user'
}

function eventTitle(event: RunEvent) {
  const titles: Record<string, string> = {
    RUN_CREATED: 'Run created',
    USER_MESSAGE_ACCEPTED: 'User message accepted',
    RUN_STARTED: 'Mock runner started',
    RUN_CANCELLING: 'Run cancelling',
    APPROVAL_REQUIRED: `Approval required: ${String(event.payload.name ?? '')}`,
    APPROVAL_RESOLVED: 'Approval resolved',
    MODEL_REQUESTED: 'Mock model requested',
    MODEL_MESSAGE_RECEIVED: 'Assistant message',
    TOOL_CALL_REQUESTED: `Tool requested: ${String(event.payload.name ?? '')}`,
    TOOL_CALL_STARTED: `Tool started: ${String(event.payload.name ?? '')}`,
    TOOL_CALL_FINISHED: `Tool finished: ${String(event.payload.name ?? '')}`,
    RUN_FINISHED: 'Run finished',
  }
  return titles[event.type] ?? event.type
}

function approvalReason(event: RunEvent) {
  const approval = event.payload.approval as { reason?: unknown } | undefined
  return typeof approval?.reason === 'string' ? approval.reason : 'This tool requires approval before it can run.'
}

function eventDetail(event: RunEvent) {
  if (typeof event.payload.content === 'string') {
    return truncate(event.payload.content, 420)
  }
  if (typeof event.payload.prompt === 'string') {
    return truncate(event.payload.prompt, 420)
  }
  return truncate(JSON.stringify(event.payload), 420)
}

function truncate(value: string, limit: number) {
  return value.length > limit ? `${value.slice(0, limit)}...` : value
}
</script>

<template>
  <main class="app-shell">
    <aside class="rail" aria-label="Runs">
      <header class="brand">
        <div class="brand-mark">CA</div>
        <div>
          <p class="eyebrow">Coding Agent</p>
          <h1>Workbench</h1>
        </div>
      </header>

      <nav class="run-list" aria-label="Run history">
        <button
          v-for="run in runHistory"
          :key="run.id"
          class="run-item"
          :class="{ active: activeRun?.id === run.id }"
          type="button"
        >
          <span>{{ run.id.slice(0, 8) }}</span>
          <small>{{ run.status.toLowerCase() }}</small>
        </button>
        <p v-if="runHistory.length === 0" class="empty-state">No runs yet.</p>
      </nav>
    </aside>

    <section class="main-pane" aria-label="Task workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Local workspace</p>
          <h2>/Users/zhumeiyuan/Desktop/CodingAgent</h2>
        </div>
        <span class="status-pill" :class="{ ready: health?.status === 'ok', pending: loading, error }">
          {{ statusLabel }}
        </span>
      </header>

      <section class="thread">
        <article v-if="!activeRun" class="message assistant-message">
          <p class="message-role">Assistant</p>
          <p>
            This workbench is wired to the backend mock runner. Submit a task to create a run, stream
            SSE events, request approval for workspace changes, and inspect resulting diffs.
          </p>
        </article>

        <article v-if="activeRun" class="message user-message">
          <p class="message-role">User</p>
          <p>{{ activePrompt }}</p>
        </article>

        <article v-if="activeRun" class="message assistant-message">
          <p class="message-role">Assistant</p>
          <p>
            Running in mock mode. This verifies the local event loop and tool registry without using
            a real model API key yet.
          </p>
        </article>

        <ol class="timeline" aria-label="Run events">
          <li v-for="item in events" :key="item.eventId">
            <span class="event-kind">{{ eventKind(item.type) }}</span>
            <div>
              <strong>{{ eventTitle(item) }}</strong>
              <p>{{ eventDetail(item) }}</p>
            </div>
          </li>
        </ol>

        <section v-if="pendingApproval" class="approval-panel" aria-label="Pending tool approval">
          <p class="eyebrow">Approval required</p>
          <h3>{{ pendingApproval.name }}</h3>
          <p>{{ pendingApproval.reason }}</p>
          <pre>{{ JSON.stringify(pendingApproval.arguments, null, 2) }}</pre>
          <div class="approval-actions">
            <button type="button" :disabled="approvalButtonDisabled" @click="approvePendingTool">
              {{ resolvingApproval ? 'Resolving' : 'Approve and run' }}
            </button>
            <button class="secondary-action danger-action" type="button" :disabled="approvalButtonDisabled" @click="rejectPendingTool">
              Reject
            </button>
          </div>
        </section>

        <p v-if="runError" class="error-text">{{ runError }}</p>

        <section class="health-panel" aria-label="Backend health">
          <p v-if="loading">Checking backend...</p>
          <p v-else-if="error" class="error-text">{{ error }}</p>
          <dl v-else-if="health" class="health-grid">
            <div>
              <dt>Service</dt>
              <dd>{{ health.service }}</dd>
            </div>
            <div>
              <dt>Java</dt>
              <dd>{{ health.javaVersion }}</dd>
            </div>
            <div>
              <dt>Server time</dt>
              <dd>{{ new Date(health.serverTime).toLocaleString() }}</dd>
            </div>
          </dl>
        </section>
      </section>

      <form class="composer" aria-label="Task composer" @submit.prevent="submitRun">
        <textarea v-model="taskDraft" aria-label="Task input" placeholder="Ask the agent to change this workspace" rows="2" />
        <div class="composer-actions">
          <button type="submit" :disabled="runButtonDisabled">
            {{ submitting ? 'Starting' : 'Run' }}
          </button>
          <button class="secondary-action" type="button" :disabled="cancelButtonDisabled" @click="cancelActiveRun">
            {{ cancelling ? 'Cancelling' : 'Cancel' }}
          </button>
        </div>
      </form>
    </section>

    <aside class="detail-pane" aria-label="Workspace details">
      <div class="tabs" role="tablist" aria-label="Detail tabs">
        <button :class="{ active: detailTab === 'files' }" type="button" @click="detailTab = 'files'">
          Files
        </button>
        <button :class="{ active: detailTab === 'diff' }" type="button" @click="detailTab = 'diff'">
          Diff
        </button>
        <button :class="{ active: detailTab === 'checks' }" type="button" @click="detailTab = 'checks'">
          Checks
        </button>
      </div>

      <section v-if="detailTab === 'files'" class="tab-body">
        <button v-for="file in files" :key="file" class="file-row" type="button">
          {{ file }}
        </button>
      </section>

      <section v-else-if="detailTab === 'diff'" class="tab-body diff-body">
        <article v-for="diff in diffs" :key="diff.path" class="diff-card">
          <strong>{{ diff.path }}</strong>
          <pre>{{ diff.diff }}</pre>
        </article>
        <p v-if="diffs.length === 0" class="empty-state">No file changes in this run yet.</p>
      </section>

      <section v-else class="tab-body">
        <div v-for="check in checks" :key="check.name" class="check-row">
          <span>{{ check.name }}</span>
          <strong>{{ check.result }}</strong>
        </div>
      </section>
    </aside>
  </main>
</template>
