<script setup lang="ts">
import { computed } from 'vue'
import type { RunEvent } from '../api/runs'
import { commandCwd, commandLine, commandResult, formatArguments, outputText, toolStatusLabel, type ToolCard } from '../run/toolCards'
import {
  checksForRun,
  diffPreview,
  filePreview,
  filesFromEvents,
  inspectorTitle,
  type InspectorSelection,
} from '../run/timeline'
import type { RunResponse } from '../api/runs'

const props = defineProps<{
  selection: InspectorSelection
  events: RunEvent[]
  toolCards: ToolCard[]
  activeRun: RunResponse | null
  healthStatus: string
}>()

const emit = defineEmits<{
  select: [selection: InspectorSelection]
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
const checks = computed(() => checksForRun(props.activeRun, props.events, props.toolCards))
const selectedCommand = computed(() => selectedTool.value?.name === 'run_command' ? commandResult(selectedTool.value) : null)
const firstDiffTool = computed(() => props.toolCards.find((card) => diffPreview(card)) ?? null)
const latestCommandTool = computed(() => [...props.toolCards].reverse().find((card) => card.name === 'run_command') ?? null)

function isToolSelection(selection: InspectorSelection): selection is Extract<InspectorSelection, { toolCallId: string }> {
  return selection.kind === 'tool' || selection.kind === 'diff' || selection.kind === 'command'
}
</script>

<template>
  <aside class="inspector-pane" aria-label="Inspector">
    <header class="inspector-header">
      <p class="section-label">Inspector</p>
      <h2>{{ title }}</h2>
      <span class="backend-chip">backend {{ props.healthStatus }}</span>
    </header>

    <nav class="inspector-tabs" aria-label="Inspector tabs">
      <button type="button" :class="{ active: props.selection.kind === 'welcome' || props.selection.kind === 'file' }" @click="emit('select', { kind: 'welcome' })">Files</button>
      <button type="button" :class="{ active: props.selection.kind === 'diff' }" @click="emit('select', firstDiffTool ? { kind: 'diff', toolCallId: firstDiffTool.id } : { kind: 'welcome' })">Diff</button>
      <button type="button" :class="{ active: props.selection.kind === 'command' }" @click="emit('select', latestCommandTool ? { kind: 'command', toolCallId: latestCommandTool.id } : { kind: 'welcome' })">Command</button>
      <button type="button" :class="{ active: props.selection.kind === 'checks' }" @click="emit('select', { kind: 'checks' })">Checks</button>
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
        <p v-else class="empty-copy">No diff selected.</p>
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

      <template v-else-if="props.selection.kind === 'checks'">
        <div class="checks-list">
          <div v-for="check in checks" :key="check.name" class="check-item" :class="`tone-${check.tone}`">
            <span>{{ check.name }}</span>
            <strong>{{ check.result }}</strong>
          </div>
        </div>
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
