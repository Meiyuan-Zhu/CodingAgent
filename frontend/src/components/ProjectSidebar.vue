<script setup lang="ts">
import type { RunResponse } from '../api/runs'

const props = defineProps<{
  activeRunId: string | null
  runs: RunResponse[]
  runTitles: Record<string, string>
}>()

const emit = defineEmits<{
  selectRun: [run: RunResponse]
  newTask: []
}>()

function shortId(id: string) {
  return id.slice(0, 8)
}

function titleFor(run: RunResponse) {
  return props.runTitles[run.id] || shortId(run.id)
}
</script>

<template>
  <aside class="sidebar" aria-label="Projects and runs">
    <header class="sidebar-brand">
      <div class="brand-dot">CA</div>
      <div>
        <div class="brand-title">Coding Agent</div>
      </div>
    </header>

    <button class="new-task-button" type="button" @click="emit('newTask')">
      <span>＋</span>
      New task
    </button>

    <section class="sidebar-section">
      <p class="section-label">Project</p>
      <div class="project-row active">
        <span class="folder-icon">▱</span>
        <span>
          <strong>CodingAgent</strong>
        </span>
      </div>
    </section>

    <section class="sidebar-section runs-section">
      <p class="section-label">Runs</p>
      <button
        v-for="run in props.runs"
        :key="run.id"
        class="run-row"
        :class="{ active: props.activeRunId === run.id }"
        type="button"
        @click="emit('selectRun', run)"
      >
        <span>
          <strong>{{ titleFor(run) }}</strong>
          <small>{{ new Date(run.createdAt).toLocaleTimeString() }}</small>
        </span>
        <em :class="`run-status-${run.status.toLowerCase()}`">{{ run.status.toLowerCase() }}</em>
      </button>
      <p v-if="props.runs.length === 0" class="empty-copy">No runs yet.</p>
    </section>
  </aside>
</template>
