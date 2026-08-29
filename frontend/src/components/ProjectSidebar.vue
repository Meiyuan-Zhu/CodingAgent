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

function statusFor(status: string) {
  const labels: Record<string, string> = {
    CREATED: '已创建',
    RUNNING: '运行中',
    WAITING_FOR_APPROVAL: '待批准',
    CANCELLING: '取消中',
    CANCELLED: '已取消',
    FAILED: '失败',
    SUCCEEDED: '完成',
  }
  return labels[status] ?? status.toLowerCase()
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
      新任务
    </button>

    <section class="sidebar-section">
      <p class="section-label">项目</p>
      <div class="project-row active">
        <span class="folder-icon">▱</span>
        <span>
          <strong>CodingAgent</strong>
        </span>
      </div>
    </section>

    <section class="sidebar-section runs-section">
      <p class="section-label">任务</p>
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
        <em :class="`run-status-${run.status.toLowerCase()}`">{{ statusFor(run.status) }}</em>
      </button>
      <p v-if="props.runs.length === 0" class="empty-copy">还没有任务。</p>
    </section>
  </aside>
</template>
