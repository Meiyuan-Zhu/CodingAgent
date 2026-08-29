import type { ToolCard } from './toolCards'
import { commandLine } from './toolCards'

export type ChangeStats = {
  additions: number
  deletions: number
}

export function toolPath(card: ToolCard) {
  const resultPath = card.result?.path
  if (typeof resultPath === 'string' && resultPath) return resultPath
  const args = card.arguments as Record<string, unknown> | null
  return typeof args?.path === 'string' && args.path ? args.path : null
}

export function basename(path: string | null) {
  if (!path) return 'workspace'
  const clean = path.replace(/\\/g, '/')
  return clean.split('/').filter(Boolean).pop() ?? clean
}

export function isChangeTool(card: ToolCard) {
  return card.name === 'write_file' || card.name === 'replace_text'
}

export function diffStats(diff: string | null | undefined): ChangeStats {
  if (!diff) return { additions: 0, deletions: 0 }
  return diff.split('\n').reduce<ChangeStats>((acc, line) => {
    if (line.startsWith('+++') || line.startsWith('---')) return acc
    if (line.startsWith('+')) acc.additions += 1
    if (line.startsWith('-')) acc.deletions += 1
    return acc
  }, { additions: 0, deletions: 0 })
}

export function changeStats(card: ToolCard): ChangeStats {
  const diff = typeof card.result?.unifiedDiff === 'string' ? card.result.unifiedDiff : null
  return diffStats(diff)
}

export function actionText(card: ToolCard) {
  if (card.name === 'run_command') {
    const verb = card.status === 'running' ? '正在运行' : card.status === 'finished' ? '已运行' : card.status === 'waiting' ? '等待批准运行' : '准备运行'
    return `${verb} ${commandLine(card)}`
  }
  if (card.name === 'read_file') {
    const verb = card.status === 'finished' ? '已读取' : card.status === 'running' ? '正在读取' : '准备读取'
    return `${verb} ${toolPath(card) ?? '文件'}`
  }
  if (card.name === 'list_files') {
    const verb = card.status === 'finished' ? '已查看文件列表' : card.status === 'running' ? '正在查看文件列表' : '准备查看文件列表'
    return verb
  }
  if (card.name === 'search_text') {
    const args = card.arguments as Record<string, unknown> | null
    const query = typeof args?.query === 'string' ? args.query : '文本'
    const verb = card.status === 'finished' ? '已搜索' : card.status === 'running' ? '正在搜索' : '准备搜索'
    return `${verb} ${query}`
  }
  const verb = card.status === 'finished' ? '已完成' : card.status === 'running' ? '正在执行' : card.status === 'waiting' ? '等待批准' : '准备执行'
  return `${verb} ${card.name}`
}

export function changeTitle(card: ToolCard) {
  const name = basename(toolPath(card))
  if (card.status === 'waiting') return `准备编辑 ${name}`
  if (card.status === 'rejected') return `已拒绝编辑 ${name}`
  if (card.status === 'running') return `正在编辑 ${name}`
  return `已编辑 ${name}`
}

export function statusTone(status: ToolCard['status']) {
  if (status === 'finished') return 'success'
  if (status === 'waiting') return 'warning'
  if (status === 'rejected') return 'danger'
  if (status === 'running') return 'live'
  return 'muted'
}
