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

  for (const event of events) {
    if (event.type === 'MODEL_MESSAGE_RECEIVED') {
      items.push({
        id: event.eventId,
        kind: 'assistant',
        title: modelTitle(event),
        content: humanModelContent(event),
        occurredAt: event.occurredAt,
        streaming: !activeRun || !terminalStatuses.has(activeRun.status),
      })
    }
    if (event.type === 'RUN_FINISHED') {
      const status = String(event.payload.status ?? activeRun?.status ?? 'finished')
      items.push({
        id: event.eventId,
        kind: 'run',
        title: status === 'SUCCEEDED' ? 'Run completed' : 'Run stopped',
        content: runFinishedContent(event),
        occurredAt: event.occurredAt,
        status,
      })
    }
  }

  for (const card of buildToolCards(events)) {
    const approvalPending = card.status === 'waiting'
    items.push({
      id: `tool-${card.id}`,
      kind: approvalPending ? 'approval' : 'tool',
      title: approvalPending ? approvalTitle(card) : toolTitle(card),
      summary: toolSummary(card),
      occurredAt: card.requestedAt,
      card,
      ...(approvalPending ? { reason: card.reason ?? 'This action needs your permission before it runs.' } : {}),
    } as TimelineItem)
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
    reason: card.reason ?? 'This action needs your permission before it runs.',
    risk: riskCopy(card.name),
    diff: diffPreview(card),
    command: card.name === 'run_command' ? commandLine(card) : null,
    cwd: card.name === 'run_command' ? commandCwd(card) : null,
  }
}

export function inspectorTitle(selection: InspectorSelection, toolCards: ToolCard[]) {
  if (selection.kind === 'tool' || selection.kind === 'diff' || selection.kind === 'command') {
    const card = toolCards.find((item) => item.id === selection.toolCallId)
    return card ? card.name : 'Inspector'
  }
  if (selection.kind === 'file') return selection.path
  return 'Workspace'
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
  return finishReason === 'TOOL_CALLS' ? 'Assistant is taking action' : 'Assistant'
}

function humanModelContent(event: RunEvent) {
  const content = typeof event.payload.content === 'string' ? event.payload.content.trim() : ''
  return content || 'The model requested a tool.'
}

function runFinishedContent(event: RunEvent) {
  const status = String(event.payload.status ?? 'finished').toLowerCase()
  const stopReason = String(event.payload.stopReason ?? 'unknown').toLowerCase()
  const rounds = event.payload.roundsUsed
  const tools = event.payload.toolCallsUsed
  return `Status: ${status}. Stop reason: ${stopReason}. Rounds: ${rounds ?? '—'}, tools: ${tools ?? '—'}.`
}

function approvalTitle(card: ToolCard) {
  if (card.name === 'run_command') return 'Permission needed to run command'
  if (card.name === 'replace_text' || card.name === 'write_file') return 'Permission needed to edit workspace'
  return `Permission needed for ${card.name}`
}

function toolTitle(card: ToolCard) {
  const verb = card.status === 'finished' ? 'Finished' : card.status === 'running' ? 'Running' : 'Proposed'
  if (card.name === 'run_command') return `${verb} command`
  if (card.name === 'read_file') return `${verb} reading file`
  if (card.name === 'list_files') return `${verb} listing files`
  if (card.name === 'replace_text') return `${verb} editing file`
  return `${verb} ${card.name}`
}

function toolSummary(card: ToolCard) {
  if (card.name === 'run_command') return commandLine(card)
  if (card.name === 'replace_text') {
    const args = card.arguments as Record<string, unknown> | null
    return typeof args?.path === 'string' ? `Change ${args.path}` : 'Change a workspace file'
  }
  if (card.status === 'finished' && card.name === 'read_file' && card.result?.path) return `Read ${String(card.result.path)}`
  return compactArguments(card.arguments)
}

function riskCopy(toolName: string) {
  if (toolName === 'run_command') return 'This will run a local process inside the configured workspace.'
  if (toolName === 'replace_text') return 'This will modify a workspace file. Review the diff before approving.'
  if (toolName === 'write_file') return 'This will create or overwrite a workspace file. Review the content before approving.'
  return 'This action requires explicit user approval before it can continue.'
}

function timeValue(value: string | null) {
  return value ? new Date(value).getTime() : 0
}
