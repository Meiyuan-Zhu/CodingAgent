<script setup lang="ts">
import { computed } from 'vue'
import type { RunEvent } from '../api/runs'
import type { WorkspaceFileEntry, WorkspaceFileResponse } from '../api/workspace'
import type { ToolCard } from '../run/toolCards'
import { basename, diffStats } from '../run/display'
import {
  diffPreview,
  inspectorTitle,
  type InspectorSelection,
} from '../run/timeline'
import UiIcon from './UiIcon.vue'

const props = defineProps<{
  open: boolean
  selection: InspectorSelection
  events: RunEvent[]
  toolCards: ToolCard[]
  undoingToolCallId: string | null
  workspaceEntriesByDirectory: Record<string, WorkspaceFileEntry[]>
  expandedDirectories: string[]
  selectedWorkspaceFile: WorkspaceFileResponse | null
  loadingWorkspacePath: string | null
  loadingWorkspaceDirectory: string | null
  workspaceError: string | null
}>()

const emit = defineEmits<{
  select: [selection: InspectorSelection]
  toggleDirectory: [path: string]
  openFile: [path: string]
  undoChange: [toolCallId: string]
  toggle: []
}>()

const title = computed(() => inspectorTitle(props.selection, props.toolCards))
const selectedTool = computed(() => {
  const selection = props.selection
  if (!isToolSelection(selection)) return null
  return props.toolCards.find((card) => card.id === selection.toolCallId) ?? null
})
const selectedDiff = computed(() => selectedTool.value ? diffPreview(selectedTool.value) : null)
const selectedFileLines = computed(() => (props.selectedWorkspaceFile?.content ?? '').split('\n'))
const reviewFiles = computed(() => buildReviewFiles(props.toolCards))
const reviewTotals = computed(() => reviewFiles.value.reduce((totals, file) => ({
  additions: totals.additions + file.activeAdditions,
  deletions: totals.deletions + file.activeDeletions,
}), { additions: 0, deletions: 0 }))
const visibleWorkspaceNodes = computed(() => buildVisibleWorkspaceNodes(
  props.workspaceEntriesByDirectory,
  new Set(props.expandedDirectories),
))

function selectReview() {
  emit('select', { kind: 'review' })
}

function selectFiles() {
  emit('select', { kind: 'welcome' })
}

function isReviewSelection(selection: InspectorSelection) {
  return selection.kind === 'review' || selection.kind === 'diff' || selection.kind === 'tool' || selection.kind === 'command'
}

function isToolSelection(selection: InspectorSelection): selection is Extract<InspectorSelection, { toolCallId: string }> {
  return selection.kind === 'diff'
}

function splitDiff(diff: string) {
  return diff.split('\n').filter((line) => line.length > 0).map((text, index) => ({
    id: `${index}-${text}`,
    text,
    tone: diffLineTone(text),
  }))
}

function buildReviewFiles(cards: ToolCard[]) {
  const files = new Map<string, {
    path: string
    additions: number
    deletions: number
    activeAdditions: number
    activeDeletions: number
    tools: ToolCard[]
    latestToolId: string
    undone: boolean
  }>()
  for (const card of cards) {
    const diff = diffPreview(card)
    if (!diff) continue
    const stats = diffStats(diff.diff)
    const existing = files.get(diff.path)
    if (existing) {
      existing.additions += stats.additions
      existing.deletions += stats.deletions
      if (!card.undone) {
        existing.activeAdditions += stats.additions
        existing.activeDeletions += stats.deletions
      }
      existing.tools.push(card)
      existing.latestToolId = card.id
      existing.undone = existing.undone && card.undone
    } else {
      files.set(diff.path, {
        path: diff.path,
        additions: stats.additions,
        deletions: stats.deletions,
        activeAdditions: card.undone ? 0 : stats.additions,
        activeDeletions: card.undone ? 0 : stats.deletions,
        tools: [card],
        latestToolId: card.id,
        undone: card.undone,
      })
    }
  }
  return [...files.values()].sort((left, right) => left.path.localeCompare(right.path))
}

function buildVisibleWorkspaceNodes(entriesByDirectory: Record<string, WorkspaceFileEntry[]>, expanded: Set<string>) {
  const nodes: Array<WorkspaceFileEntry & { depth: number; directoryPath: string; expanded: boolean }> = []
  appendDirectory('.', 0)
  return nodes

  function appendDirectory(directory: string, depth: number) {
    const entries = entriesByDirectory[directory] ?? []
    const sorted = [...entries].sort((left, right) => {
      if (left.type === 'DIRECTORY' && right.type !== 'DIRECTORY') return -1
      if (left.type !== 'DIRECTORY' && right.type === 'DIRECTORY') return 1
      return left.path.localeCompare(right.path)
    })
    for (const entry of sorted) {
      const isDirectory = entry.type === 'DIRECTORY'
      const directoryPath = isDirectory ? entry.path : parentPath(entry.path)
      const isExpanded = isDirectory && expanded.has(entry.path)
      nodes.push({ ...entry, depth, directoryPath, expanded: isExpanded })
      if (isDirectory && isExpanded) {
        appendDirectory(entry.path, depth + 1)
      }
    }
  }
}

function parentPath(path: string) {
  const slash = path.lastIndexOf('/')
  return slash >= 0 ? path.slice(0, slash) : '.'
}

function statsLabel(additions: number, deletions: number) {
  return `+${additions} -${deletions}`
}

function reviewFileStatsLabel(file: { activeAdditions: number; activeDeletions: number; undone: boolean }) {
  return file.undone ? '已撤销' : statsLabel(file.activeAdditions, file.activeDeletions)
}

function reviewToolStatsLabel(tool: ToolCard) {
  if (tool.undone) return '已撤销'
  const diff = diffPreview(tool)
  const stats = diffStats(diff?.diff)
  return statsLabel(stats.additions, stats.deletions)
}

function diffLineTone(line: string) {
  if (line.startsWith('+++') || line.startsWith('---')) return 'file'
  if (line.startsWith('@@')) return 'hunk'
  if (line.startsWith('+')) return 'add'
  if (line.startsWith('-')) return 'remove'
  return 'context'
}

function languageBadge(path: string | null | undefined) {
  if (!path) return 'TXT'
  const extension = path.split('.').pop()?.toLowerCase()
  const labels: Record<string, string> = {
    css: 'CSS',
    html: 'HTML',
    java: 'JAVA',
    js: 'JS',
    json: 'JSON',
    md: 'MD',
    py: 'PY',
    cpp: 'C++',
    cc: 'C++',
    hpp: 'H++',
    h: 'H',
    ts: 'TS',
    vue: 'VUE',
    xml: 'XML',
    yml: 'YML',
    yaml: 'YAML',
  }
  return extension ? labels[extension] ?? extension.slice(0, 4).toUpperCase() : 'TXT'
}
</script>

<template>
  <aside class="inspector-pane" :class="{ closed: !props.open }" aria-label="工作区审查面板">
    <header class="inspector-header">
      <div>
        <p class="section-label">面板</p>
        <h2>{{ title }}</h2>
      </div>
      <button class="panel-icon-button" type="button" aria-label="收起右侧面板" @click="emit('toggle')"><UiIcon name="chevron-right" /></button>
    </header>

    <nav class="inspector-tabs compact-tabs" aria-label="审查面板切换">
      <button type="button" :class="{ active: isReviewSelection(props.selection) }" @click="selectReview"><UiIcon name="review" />审查</button>
      <button type="button" :class="{ active: props.selection.kind === 'welcome' || props.selection.kind === 'file' }" @click="selectFiles"><UiIcon name="file" />文件</button>
    </nav>

    <section class="inspector-body">
      <template v-if="isReviewSelection(props.selection)">
        <article class="review-surface">
          <header class="review-toolbar review-summary">
            <span>修改位置</span>
            <div class="review-toolbar-actions">
              <b>+{{ reviewTotals.additions }}</b>
              <i>-{{ reviewTotals.deletions }}</i>
            </div>
          </header>

          <section v-if="reviewFiles.length" class="review-file-list" aria-label="本对话修改过的文件">
            <article
              v-for="file in reviewFiles"
              :key="file.path"
              class="review-file-item"
              :class="{ active: selectedDiff?.path === file.path, undone: file.undone }"
            >
              <button type="button" @click="emit('select', { kind: 'diff', toolCallId: file.latestToolId })">
                <span class="file-language">{{ languageBadge(file.path) }}</span>
                <strong>{{ file.path }}</strong>
                <em>{{ reviewFileStatsLabel(file) }}</em>
              </button>

              <div v-if="selectedDiff?.path === file.path" class="review-file-diffs">
                <section
                  v-for="tool in file.tools"
                  :key="tool.id"
                  class="review-diff-block"
                  :class="{ undone: tool.undone }"
                >
                  <header class="review-file-header">
                    <strong>{{ diffPreview(tool)?.path }}</strong>
                    <em>{{ reviewToolStatsLabel(tool) }}</em>
                    <button
                      v-if="tool.undoable || tool.undone"
                      class="undo-button"
                      type="button"
                      :disabled="tool.undone || props.undoingToolCallId === tool.id"
                      :aria-label="tool.undone ? '变更已撤销' : '撤销这次文件变更'"
                      @click="emit('undoChange', tool.id)"
                    >
                      <UiIcon name="undo" />
                      {{ tool.undone ? '已撤销' : props.undoingToolCallId === tool.id ? '撤销中' : '撤销' }}
                    </button>
                  </header>
                  <ol class="diff-lines" aria-label="Unified diff">
                    <li
                      v-for="(line, index) in splitDiff(diffPreview(tool)?.diff ?? '')"
                      :key="line.id"
                      :class="`diff-${line.tone}`"
                    >
                      <span>{{ index + 1 }}</span>
                      <code>{{ line.text }}</code>
                    </li>
                  </ol>
                </section>
              </div>
            </article>
          </section>
          <p v-else class="empty-copy panel-empty">这个对话还没有文件变更。</p>
        </article>
      </template>

      <template v-else>
        <section class="workspace-file-browser" :class="{ previewing: props.selection.kind === 'file' }">
          <aside class="workspace-tree" aria-label="Workspace 文件目录">
            <button
              v-for="node in visibleWorkspaceNodes"
              :key="node.path"
              type="button"
              class="workspace-tree-row"
              :class="{ active: props.selection.kind === 'file' && props.selection.path === node.path }"
              :style="{ paddingLeft: `${8 + node.depth * 16}px` }"
              @click="node.type === 'DIRECTORY' ? emit('toggleDirectory', node.path) : emit('openFile', node.path)"
            >
              <UiIcon :name="node.type === 'DIRECTORY' ? 'folder' : 'file'" />
              <strong>{{ basename(node.path) }}</strong>
              <small v-if="node.type === 'DIRECTORY'">{{ node.expanded ? '展开' : '' }}</small>
              <small v-else-if="node.sizeBytes !== null">{{ node.sizeBytes }} B</small>
            </button>
            <p v-if="props.workspaceError" class="empty-copy panel-empty error-copy">{{ props.workspaceError }}</p>
            <p v-else-if="visibleWorkspaceNodes.length === 0 && props.loadingWorkspaceDirectory" class="empty-copy panel-empty">正在加载 workspace 文件。</p>
            <p v-else-if="visibleWorkspaceNodes.length === 0" class="empty-copy panel-empty">这个项目还没有文件。</p>
          </aside>

          <article v-if="props.selection.kind === 'file'" class="file-preview-pane">
            <header class="review-toolbar">
              <span class="file-language">{{ languageBadge(props.selection.path) }}</span>
              <strong>{{ props.selection.path }}</strong>
              <em v-if="props.selectedWorkspaceFile">{{ props.selectedWorkspaceFile.sizeBytes }} bytes</em>
            </header>
            <p v-if="props.loadingWorkspacePath === props.selection.path" class="empty-copy panel-empty">正在读取文件。</p>
            <ol v-else-if="props.selectedWorkspaceFile?.content" class="file-lines" aria-label="File preview">
              <li v-for="(line, index) in selectedFileLines" :key="`${index}-${line}`">
                <span>{{ index + 1 }}</span>
                <code>{{ line || ' ' }}</code>
              </li>
            </ol>
            <p v-else-if="props.workspaceError" class="empty-copy panel-empty error-copy">{{ props.workspaceError }}</p>
            <p v-else class="empty-copy panel-empty">文件内容为空。</p>
          </article>
        </section>
      </template>
    </section>
  </aside>
</template>
