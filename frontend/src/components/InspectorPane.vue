<script setup lang="ts">
import { computed } from 'vue'
import type { RunEvent } from '../api/runs'
import { commandCwd, commandLine, commandResult, formatArguments, outputText, type ToolCard } from '../run/toolCards'
import { basename, diffStats } from '../run/display'
import {
  diffPreview,
  filePreview,
  filesFromEvents,
  inspectorTitle,
  type InspectorSelection,
} from '../run/timeline'

const props = defineProps<{
  open: boolean
  selection: InspectorSelection
  events: RunEvent[]
  toolCards: ToolCard[]
}>()

const emit = defineEmits<{
  select: [selection: InspectorSelection]
  toggle: []
}>()

const title = computed(() => inspectorTitle(props.selection, props.toolCards))
const files = computed(() => filesFromEvents(props.events))
const selectedTool = computed(() => {
  const selection = props.selection
  if (!isToolSelection(selection)) return null
  return props.toolCards.find((card) => card.id === selection.toolCallId) ?? null
})
const selectedDiff = computed(() => selectedTool.value ? diffPreview(selectedTool.value) : null)
const selectedDiffStats = computed(() => diffStats(selectedDiff.value?.diff))
const selectedDiffLines = computed(() => splitDiff(selectedDiff.value?.diff ?? ''))
const selectedFile = computed(() => props.selection.kind === 'file' ? filePreview(props.events, props.selection.path) : null)
const selectedFileLines = computed(() => (selectedFile.value?.content ?? '').split('\n'))
const selectedCommand = computed(() => selectedTool.value?.name === 'run_command' ? commandResult(selectedTool.value) : null)
const firstDiffTool = computed(() => props.toolCards.find((card) => diffPreview(card)) ?? null)
const latestCommandTool = computed(() => [...props.toolCards].reverse().find((card) => card.name === 'run_command') ?? null)
const firstReviewTool = computed(() => firstDiffTool.value ?? latestCommandTool.value ?? props.toolCards[0] ?? null)

function selectReview() {
  const card = firstReviewTool.value
  if (!card) {
    emit('select', { kind: 'diff', toolCallId: 'none' })
    return
  }
  emit('select', diffPreview(card) ? { kind: 'diff', toolCallId: card.id } : card.name === 'run_command' ? { kind: 'command', toolCallId: card.id } : { kind: 'tool', toolCallId: card.id })
}

function selectFiles() {
  emit('select', { kind: 'welcome' })
}

function isReviewSelection(selection: InspectorSelection) {
  return selection.kind === 'tool' || selection.kind === 'diff' || selection.kind === 'command'
}

function isToolSelection(selection: InspectorSelection): selection is Extract<InspectorSelection, { toolCallId: string }> {
  return selection.kind === 'tool' || selection.kind === 'diff' || selection.kind === 'command'
}

function splitDiff(diff: string) {
  return diff.split('\n').filter((line) => line.length > 0).map((text, index) => ({
    id: `${index}-${text}`,
    text,
    tone: diffLineTone(text),
  }))
}

function diffLineTone(line: string) {
  if (line.startsWith('+++') || line.startsWith('---')) return 'file'
  if (line.startsWith('@@')) return 'hunk'
  if (line.startsWith('+')) return 'add'
  if (line.startsWith('-')) return 'remove'
  return 'context'
}
</script>

<template>
  <aside class="inspector-pane" :class="{ closed: !props.open }" aria-label="Workspace panel">
    <header class="inspector-header">
      <div>
        <p class="section-label">Workspace</p>
        <h2>{{ title }}</h2>
      </div>
      <button class="panel-icon-button" type="button" aria-label="收起右侧面板" @click="emit('toggle')">〉</button>
    </header>

    <nav class="inspector-tabs compact-tabs" aria-label="Workspace panel tabs">
      <button type="button" :class="{ active: isReviewSelection(props.selection) }" @click="selectReview"><span>▣</span>审查</button>
      <button type="button" :class="{ active: props.selection.kind === 'welcome' || props.selection.kind === 'file' }" @click="selectFiles"><span>□</span>文件</button>
    </nav>

    <section class="inspector-body">
      <template v-if="props.selection.kind === 'diff'">
        <article v-if="selectedDiff" class="review-surface">
          <header class="review-toolbar">
            <span>上一轮</span>
            <b>+{{ selectedDiffStats.additions }}</b>
            <i>-{{ selectedDiffStats.deletions }}</i>
          </header>
          <section class="review-file-header">
            <span class="file-language">C</span>
            <strong>{{ selectedDiff.path }}</strong>
            <em>+{{ selectedDiffStats.additions }} -{{ selectedDiffStats.deletions }}</em>
          </section>
          <ol class="diff-lines" aria-label="Unified diff">
            <li v-for="(line, index) in selectedDiffLines" :key="line.id" :class="`diff-${line.tone}`">
              <span>{{ index + 1 }}</span>
              <code>{{ line.text }}</code>
            </li>
          </ol>
        </article>
        <p v-else class="empty-copy panel-empty">Agent 提出文件变更后，diff 会出现在这里。</p>
      </template>

      <template v-else-if="props.selection.kind === 'command'">
        <article v-if="selectedTool" class="inspector-card command-inspector">
          <p class="mini-label">Command</p>
          <h3>{{ commandLine(selectedTool) }}</h3>
          <dl class="fact-list">
            <div><dt>cwd</dt><dd>{{ commandCwd(selectedTool) }}</dd></div>
            <div><dt>exit</dt><dd>{{ selectedCommand?.exitCode ?? '—' }}</dd></div>
          </dl>
          <section class="terminal-output"><strong>stdout</strong><pre>{{ outputText(selectedCommand?.stdout) }}</pre></section>
          <section class="terminal-output stderr"><strong>stderr</strong><pre>{{ outputText(selectedCommand?.stderr) }}</pre></section>
        </article>
        <p v-else class="empty-copy panel-empty">暂无命令记录。</p>
      </template>

      <template v-else-if="props.selection.kind === 'tool'">
        <article v-if="selectedTool" class="inspector-card">
          <p class="mini-label">Tool details</p>
          <h3>{{ selectedTool.name }}</h3>
          <pre>{{ formatArguments(selectedTool.arguments) }}</pre>
          <pre v-if="selectedTool.rawContent">{{ selectedTool.rawContent }}</pre>
        </article>
        <p v-else class="empty-copy panel-empty">暂无可审查的工具记录。</p>
      </template>

      <template v-else-if="props.selection.kind === 'file'">
        <article class="review-surface file-review">
          <header class="review-toolbar">
            <span>文件</span>
            <strong>{{ basename(props.selection.path) }}</strong>
          </header>
          <section class="review-file-header">
            <span class="file-language">txt</span>
            <strong>{{ props.selection.path }}</strong>
            <em v-if="selectedFile?.sizeBytes !== null">{{ selectedFile?.sizeBytes }} bytes</em>
          </section>
          <ol v-if="selectedFile?.content" class="file-lines" aria-label="File preview">
            <li v-for="(line, index) in selectedFileLines" :key="`${index}-${line}`">
              <span>{{ index + 1 }}</span>
              <code>{{ line || ' ' }}</code>
            </li>
          </ol>
          <p v-else class="empty-copy panel-empty">文件已被发现，但内容还没有被读取。</p>
        </article>
      </template>

      <template v-else>
        <section class="file-list">
          <button v-for="file in files" :key="file.path" type="button" @click="emit('select', { kind: 'file', path: file.path })">
            <span>{{ file.type === 'DIRECTORY' ? '▸' : '·' }}</span>
            <strong>{{ file.path }}</strong>
            <small v-if="file.sizeBytes !== null">{{ file.sizeBytes }} bytes</small>
          </button>
          <p v-if="files.length === 0" class="empty-copy panel-empty">Agent 查看 workspace 后，文件会出现在这里。</p>
        </section>
      </template>
    </section>
  </aside>
</template>
