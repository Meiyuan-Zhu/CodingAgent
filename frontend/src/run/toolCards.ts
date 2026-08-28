import type { RunEvent } from '../api/runs'

export type ToolCardStatus = 'requested' | 'waiting' | 'running' | 'finished' | 'rejected'

export type ToolCard = {
  id: string
  name: string
  status: ToolCardStatus
  arguments: unknown
  reason: string | null
  approved: boolean | null
  requestedAt: string | null
  startedAt: string | null
  finishedAt: string | null
  result: Record<string, unknown> | null
  rawContent: string
  error: string | null
}

export type CommandResult = {
  command?: string[]
  cwd?: string
  exitCode?: number
  stdout?: string
  stderr?: string
  stdoutTruncated?: boolean
  stderrTruncated?: boolean
  durationMillis?: number
}

export function buildToolCards(events: RunEvent[]) {
  const cards = new Map<string, ToolCard>()

  for (const event of events) {
    if (!event.type.startsWith('TOOL_CALL') && !event.type.startsWith('APPROVAL')) continue
    const toolCallId = String(event.payload.toolCallId ?? event.payload.id ?? '')
    if (!toolCallId) continue
    const card = ensureToolCard(cards, toolCallId)

    if (typeof event.payload.name === 'string' && event.payload.name) {
      card.name = event.payload.name
    }
    if ('arguments' in event.payload) {
      card.arguments = event.payload.arguments
    }

    if (event.type === 'TOOL_CALL_REQUESTED') {
      card.status = 'requested'
      card.requestedAt = event.occurredAt
    }
    if (event.type === 'APPROVAL_REQUIRED') {
      card.status = 'waiting'
      card.requestedAt ??= event.occurredAt
      card.reason = approvalReason(event)
    }
    if (event.type === 'APPROVAL_RESOLVED') {
      card.approved = Boolean(event.payload.approved)
      card.status = card.approved ? 'requested' : 'rejected'
    }
    if (event.type === 'TOOL_CALL_STARTED') {
      card.status = 'running'
      card.startedAt = event.occurredAt
    }
    if (event.type === 'TOOL_CALL_FINISHED') {
      card.status = 'finished'
      card.finishedAt = event.occurredAt
      card.rawContent = typeof event.payload.content === 'string' ? event.payload.content : ''
      card.result = parseJsonObject(card.rawContent)
      card.error = typeof event.payload.errorMessage === 'string' ? event.payload.errorMessage : null
    }
  }

  return [...cards.values()]
}

export function approvalReason(event: RunEvent) {
  const approval = event.payload.approval as { reason?: unknown } | undefined
  return typeof approval?.reason === 'string' ? approval.reason : 'This tool requires approval before it can run.'
}

export function parseJsonObject(value: string): Record<string, unknown> | null {
  if (!value) return null
  try {
    const parsed = JSON.parse(value) as unknown
    return parsed && typeof parsed === 'object' && !Array.isArray(parsed) ? (parsed as Record<string, unknown>) : null
  } catch {
    return null
  }
}

export function formatArguments(value: unknown) {
  if (value === null || value === undefined) return '{}'
  return JSON.stringify(value, null, 2)
}

export function compactArguments(value: unknown) {
  if (!value || typeof value !== 'object') return '{}'
  const args = value as Record<string, unknown>
  if (Array.isArray(args.command)) {
    const command = args.command.map((part) => String(part)).join(' ')
    const cwd = typeof args.cwd === 'string' ? args.cwd : '.'
    return `${command}  ·  cwd: ${cwd}`
  }
  if (typeof args.path === 'string') return args.path
  if (typeof args.query === 'string') return `query: ${args.query}`
  return truncate(JSON.stringify(args), 140)
}

export function toolStatusLabel(status: ToolCardStatus) {
  const labels: Record<ToolCardStatus, string> = {
    requested: 'Proposed',
    waiting: 'Waiting approval',
    running: 'Running',
    finished: 'Finished',
    rejected: 'Rejected',
  }
  return labels[status]
}

export function commandResult(card: ToolCard): CommandResult | null {
  if (!card.result || card.name !== 'run_command') return null
  return card.result as CommandResult
}

export function commandLine(card: ToolCard) {
  const result = commandResult(card)
  if (result?.command?.length) return result.command.join(' ')
  const args = card.arguments as Record<string, unknown> | null
  if (args && Array.isArray(args.command)) return args.command.map((part) => String(part)).join(' ')
  return compactArguments(card.arguments)
}

export function commandCwd(card: ToolCard) {
  const result = commandResult(card)
  if (typeof result?.cwd === 'string') return result.cwd
  const args = card.arguments as Record<string, unknown> | null
  return typeof args?.cwd === 'string' ? args.cwd : '.'
}

export function outputText(value: unknown) {
  return typeof value === 'string' && value.length > 0 ? value : '∅'
}

export function formatDuration(value: unknown) {
  return typeof value === 'number' ? `${value} ms` : '—'
}

export function formatOccurredAt(value: string | null) {
  if (!value) return '—'
  return new Date(value).toLocaleTimeString()
}

function ensureToolCard(cards: Map<string, ToolCard>, id: string) {
  const existing = cards.get(id)
  if (existing) return existing
  const card: ToolCard = {
    id,
    name: 'tool',
    status: 'requested',
    arguments: null,
    reason: null,
    approved: null,
    requestedAt: null,
    startedAt: null,
    finishedAt: null,
    result: null,
    rawContent: '',
    error: null,
  }
  cards.set(id, card)
  return card
}

function truncate(value: string, limit: number) {
  return value.length > limit ? `${value.slice(0, limit)}...` : value
}
