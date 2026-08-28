<script setup lang="ts">
import { computed } from 'vue'
import type { RunEvent } from '../api/runs'
import { commandCwd, commandLine, commandResult, formatArguments, outputText, toolStatusLabel, type ToolCard } from '../run/toolCards'
import {
  diffPreview,
  filePreview,
  filesFromEvents,
  inspectorTitle,
  type InspectorSelection,
} from '../run/timeline'
import type { RunResponse } from '../api/runs'

const props = defineProps<{
  open: boolean
  selection: InspectorSelection
  events: RunEvent[]
  toolCards: ToolCard[]
  activeRun: RunResponse | null
  healthStatus: string
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
const selectedFile = computed(() => props.selection.kind === 'file' ? filePreview(props.events, props.selection.path) : null)
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
</script>

<template>
  <aside class="inspector-pane" :class="{ closed: !props.open }" aria-label="Inspector">
    <header class="inspector-header">
      <p class="section-label">Panel</p>
      <h2>{{ title }}</h2>
      <div class="inspector-header-actions">
        <span class="backend-chip">backend {{ props.healthStatus }}</span>
        <button class="panel-icon-button" type="button" aria-label="Hide inspector" @click="emit('toggle')">〉</button>
      </div>
    </header>

    <nav class="inspector-tabs compact-tabs" aria-label="Workspace panel tabs">
      <button type="button" :class="{ active: isReviewSelection(props.selection) }" @click="selectReview">审查</button>
      <button type="button" :class="{ active: props.selection.kind === 'welcome' || props.selection.kind === 'file' }" @click="selectFiles">文件</button>
    </nav>

    <section class="inspector-body">
      <template v-if="props.selection.kind === 'tool'">
        <article v-if="selectedTool" class="inspector-card">
          <p class="mini-label">Tool call</p>
          <h3>{{ selectedTool.name }}</h3>
          <dl class="fact-list">
            <div><dt>Status</dt><dd>{{ toolStatusLabel(selectedTool.status) }}</dd></div>
            <div><dt>Call ID</dt><dd>{{ selectedTool.id }}</dd></div>
          </dl>
          <pre>{{ formatArguments(selectedTool.arguments) }}</pre>
          <pre v-if="selectedTool.rawContent">{{ selectedTool.rawContent }}</pre>
        </article>
      </template>

      <template v-else-if="props.selection.kind === 'diff'">
        <article v-if="selectedDiff" class="inspector-card diff-inspector">
          <p class="mini-label">Proposed change</p>
          <h3>{{ selectedDiff.path }}</h3>
          <pre>{{ selectedDiff.diff }}</pre>
        </article>
        <p v-else class="empty-copy">No diff yet. Changes will appear here after the agent proposes an edit.</p>
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
        <p v-else class="empty-copy">No command selected.</p>
      </template>

      <template v-else-if="props.selection.kind === 'file'">
        <article class="inspector-card file-preview">
          <p class="mini-label">File</p>
          <h3>{{ props.selection.path }}</h3>
          <p v-if="selectedFile?.sizeBytes !== null" class="file-meta">{{ selectedFile?.sizeBytes }} bytes</p>
          <pre v-if="selectedFile?.content">{{ selectedFile.content }}</pre>
          <p v-else class="empty-copy">This file was discovered, but its content has not been read yet.</p>
        </article>
      </template>


      <template v-else>
        <section class="file-list">
          <button v-for="file in files" :key="file.path" type="button" @click="emit('select', { kind: 'file', path: file.path })">
            <span>{{ file.type === 'DIRECTORY' ? '▸' : '·' }}</span>
            <strong>{{ file.path }}</strong>
            <small v-if="file.sizeBytes !== null">{{ file.sizeBytes }} bytes</small>
          </button>
          <p v-if="files.length === 0" class="empty-copy">Files will appear here after the agent inspects the workspace.</p>
        </section>
      </template>
    </section>
  </aside>
</template>
