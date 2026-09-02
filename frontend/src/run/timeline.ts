import type { RunEvent, RunResponse } from '../api/runs'
import {
  buildToolCards,
  commandCwd,
  commandLine,
  compactArguments,
  parseJsonObject,
  type ToolCard,
} from './toolCards'

export type TimelineItem =
  | {
      id: string
      kind: 'user'
      title: string
      content: string
      occurredAt: string | null
    }
  | {
      id: string
      kind: 'assistant'
      title: string
      content: string
      occurredAt: string | null
      streaming: boolean
    }
  | {
      id: string
      kind: 'thinking'
      content: string
      occurredAt: string | null
    }
  | {
      id: string
      kind: 'tool'
      title: string
      summary: string
      occurredAt: string | null
      card: ToolCard
    }
  | {
      id: string
      kind: 'approval'
      title: string
      summary: string
      occurredAt: string | null
      card: ToolCard
      reason: string
    }
  | {
      id: string
      kind: 'run'
      title: string
      content: string
      occurredAt: string | null
      status: string
    }

export type InspectorSelection =
  | { kind: 'welcome' }
  | { kind: 'review' }
  | { kind: 'tool'; toolCallId: string }
  | { kind: 'diff'; toolCallId: string }
  | { kind: 'command'; toolCallId: string }
  | { kind: 'file'; path: string }

export type FilePreview = {
  path: string
  content: string | null
  sizeBytes: number | null
}

export type FileEntry = {
  path: string
  type: string
  sizeBytes: number | null
}

export type DiffPreview = {
  path: string
  diff: string
}

export type ApprovalView = {
  toolCallId: string
  name: string
  arguments: unknown
  reason: string
  risk: string
  diff: DiffPreview | null
  command: string | null
  cwd: string | null
}

const terminalStatuses = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED'])

export function buildTimelineItems(events: RunEvent[], activePrompt: string, activeRun: RunResponse | null): TimelineItem[] {
  const items: TimelineItem[] = []
  const userEvent = events.find((event) => event.type === 'USER_MESSAGE_ACCEPTED')
  const prompt = activePrompt || (typeof userEvent?.payload.prompt === 'string' ? userEvent.payload.prompt : '')
  if (prompt) {
    items.push({
      id: userEvent?.eventId ?? 'active-user-message',
      kind: 'user',
      title: 'You',
      content: prompt,
      occurredAt: userEvent?.occurredAt ?? activeRun?.createdAt ?? null,
    })
  }

  const hasFinishedEvent = events.some((event) => event.type === 'RUN_FINISHED')
  const isActive = !!activeRun && !hasFinishedEvent && !terminalStatuses.has(activeRun.status)
  const modelDeltas = modelDeltaEntries(events)
  const visibleAssistantEvents = events
    .filter((event) => event.type === 'MODEL_MESSAGE_RECEIVED')
    .map((event) => ({ event, content: humanModelContent(event) }))
    .filter((entry) => entry.content.length > 0)
  const assistantEntries = isActive ? visibleAssistantEvents : visibleAssistantEvents.slice(-1)
  const finalRounds = new Set(visibleAssistantEvents.map(({ event }) => Number(event.payload.round ?? 0)))

  for (const entry of modelDeltas) {
    if (finalRounds.has(entry.round)) continue
    items.push({
      id: `model-delta-${entry.round}`,
      kind: 'assistant',
      title: 'Agent',
      content: entry.content,
      occurredAt: entry.occurredAt,
      streaming: true,
    })
  }

  for (const { event, content } of assistantEntries) {
    items.push({
      id: event.eventId,
      kind: 'assistant',
      title: modelTitle(event),
      content,
      occurredAt: event.occurredAt,
      streaming: isActive,
    })
  }

  for (const event of events) {
    if (event.type !== 'RUN_FINISHED') continue
    const status = String(event.payload.status ?? activeRun?.status ?? 'finished')
    items.push({
      id: event.eventId,
      kind: 'run',
      title: status === 'SUCCEEDED' ? '已完成' : '已停止',
      content: runFinishedContent(event),
      occurredAt: event.occurredAt,
      status,
    })
  }

  const toolCards = buildToolCards(events)
  for (const card of toolCards) {
    const approvalPending = card.status === 'waiting'
    items.push({
      id: `tool-${card.id}`,
      kind: approvalPending ? 'approval' : 'tool',
      title: approvalPending ? approvalTitle(card) : toolTitle(card),
      summary: toolSummary(card),
      occurredAt: card.requestedAt,
      card,
      ...(approvalPending ? { reason: card.reason ?? '执行前需要你的批准。' } : {}),
    } as TimelineItem)
  }

  const hasStarted = events.some((event) => event.type === 'RUN_STARTED' || event.type === 'MODEL_REQUESTED')
  if (isActive && hasStarted && visibleAssistantEvents.length === 0 && modelDeltas.length === 0 && toolCards.length === 0) {
    items.push({
      id: 'thinking-active',
      kind: 'thinking',
      content: '正在思考',
      occurredAt: events.find((event) => event.type === 'RUN_STARTED')?.occurredAt ?? activeRun.createdAt,
    })
  }

  return items.sort((left, right) => timeValue(left.occurredAt) - timeValue(right.occurredAt))
}

export function pendingApprovalView(toolCards: ToolCard[]): ApprovalView | null {
  const card = [...toolCards].reverse().find((item) => item.status === 'waiting')
  if (!card) return null
  return approvalView(card)
}

export function approvalView(card: ToolCard): ApprovalView {
  return {
    toolCallId: card.id,
    name: card.name,
    arguments: card.arguments,
    reason: card.reason ?? '执行前需要你的批准。',
    risk: riskCopy(card.name),
    diff: diffPreview(card),
    command: card.name === 'run_command' ? commandLine(card) : null,
    cwd: card.name === 'run_command' ? commandCwd(card) : null,
  }
}

export function filesFromEvents(events: RunEvent[]): FileEntry[] {
  const files = new Map<string, FileEntry>()
  for (const event of events) {
    if (event.type !== 'TOOL_CALL_FINISHED') continue
    const parsed = parseJsonObject(typeof event.payload.content === 'string' ? event.payload.content : '')
    const listed = parsed?.files
    if (Array.isArray(listed)) {
      for (const item of listed) {
        if (!item || typeof item !== 'object') continue
        const file = item as Record<string, unknown>
        if (typeof file.path !== 'string') continue
        files.set(file.path, {
          path: file.path,
          type: typeof file.type === 'string' ? file.type : 'FILE',
          sizeBytes: typeof file.sizeBytes === 'number' ? file.sizeBytes : null,
        })
      }
    }
    if (typeof parsed?.path === 'string' && typeof parsed.content === 'string') {
      files.set(parsed.path, {
        path: parsed.path,
        type: 'FILE',
        sizeBytes: typeof parsed.sizeBytes === 'number' ? parsed.sizeBytes : null,
      })
    }
  }
  return [...files.values()].sort((left, right) => left.path.localeCompare(right.path))
}

export function filePreview(events: RunEvent[], path: string): FilePreview | null {
  for (const event of [...events].reverse()) {
    if (event.type !== 'TOOL_CALL_FINISHED') continue
    const parsed = parseJsonObject(typeof event.payload.content === 'string' ? event.payload.content : '')
    if (parsed?.path === path && typeof parsed.content === 'string') {
      return {
        path,
        content: parsed.content,
        sizeBytes: typeof parsed.sizeBytes === 'number' ? parsed.sizeBytes : null,
      }
    }
  }
  return null
}

export function diffPreview(card: ToolCard): DiffPreview | null {
  if (!card.result || typeof card.result.unifiedDiff !== 'string') return null
  return {
    path: typeof card.result.path === 'string' ? card.result.path : compactArguments(card.arguments),
    diff: card.result.unifiedDiff,
  }
}

function modelTitle(event: RunEvent) {
  const finishReason = String(event.payload.finishReason ?? '')
  return finishReason === 'TOOL_CALLS' ? 'Agent 正在执行' : 'Agent'
}

function humanModelContent(event: RunEvent) {
  const content = typeof event.payload.content === 'string' ? event.payload.content.trim() : ''
  if (!content) return ''
  if (/^model requested tool execution\.?$/i.test(content)) return ''
  if (/^the model requested a tool\.?$/i.test(content)) return ''
  return content
}

function modelDeltaEntries(events: RunEvent[]) {
  const byRound = new Map<number, { round: number; content: string; occurredAt: string | null }>()
  for (const event of events) {
    if (event.type !== 'MODEL_MESSAGE_DELTA') continue
    const delta = typeof event.payload.delta === 'string' ? event.payload.delta : ''
    if (!delta) continue
    const round = typeof event.payload.round === 'number' ? event.payload.round : 0
    const existing = byRound.get(round)
    if (existing) {
      existing.content += delta
    } else {
      byRound.set(round, { round, content: delta, occurredAt: event.occurredAt })
    }
  }
  return [...byRound.values()].filter((entry) => entry.content.trim().length > 0)
}

function runFinishedContent(event: RunEvent) {
  const status = String(event.payload.status ?? 'finished')
  const stopReason = String(event.payload.stopReason ?? '').toLowerCase()
  const errorMessage = typeof event.payload.errorMessage === 'string' ? conciseError(event.payload.errorMessage) : ''
  if (status === 'SUCCEEDED') return '任务已完成。变更和验证结果可在审查面板中查看。'
  if (status === 'CANCELLED') return '任务已取消。'
  if (stopReason && errorMessage) return `任务停止：${stopReason}\n${errorMessage}`
  if (stopReason) return `任务停止：${stopReason}`
  return '任务已停止。'
}

function conciseError(value: string) {
  const trimmed = value.trim().replace(/\s+/g, ' ')
  if (!trimmed) return ''
  return trimmed.length > 220 ? `${trimmed.slice(0, 220)}...` : trimmed
}

function approvalTitle(card: ToolCard) {
  if (card.name === 'run_command') return '需要批准运行命令'
  if (card.name === 'replace_text' || card.name === 'edit_file' || card.name === 'write_file') return '需要批准编辑 workspace'
  return `需要批准执行 ${card.name}`
}

function toolTitle(card: ToolCard) {
  const verb = card.status === 'finished' ? '已完成' : card.status === 'running' ? '正在执行' : '已提出'
  if (card.name === 'run_command') return `${verb}命令`
  if (card.name === 'read_file') return `${verb}读取文件`
  if (card.name === 'list_files') return `${verb}查看文件`
  if (card.name === 'replace_text' || card.name === 'edit_file') return `${verb}编辑文件`
  return `${verb} ${card.name}`
}

function toolSummary(card: ToolCard) {
  if (card.name === 'run_command') return commandLine(card)
  if (card.name === 'replace_text' || card.name === 'edit_file') {
    const args = card.arguments as Record<string, unknown> | null
    return typeof args?.path === 'string' ? `修改 ${args.path}` : '修改 workspace 文件'
  }
  if (card.status === 'finished' && card.name === 'read_file' && card.result?.path) return `读取 ${String(card.result.path)}`
  return compactArguments(card.arguments)
}

function riskCopy(toolName: string) {
  if (toolName === 'run_command') return 'Agent 想在本地 workspace 内运行命令。批准前请确认命令和工作目录。'
  if (toolName === 'replace_text' || toolName === 'edit_file') return 'Agent 想修改 workspace 文件。批准前请先查看 diff。'
  if (toolName === 'write_file') return 'Agent 想创建或覆盖 workspace 文件。批准前请先查看拟写入内容。'
  return '这个动作需要你明确批准后才会继续。'
}

function timeValue(value: string | null) {
  return value ? new Date(value).getTime() : 0
}
