<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchHealth, type HealthResponse } from './api/health'

const health = ref<HealthResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)
const detailTab = ref<'files' | 'diff' | 'checks'>('files')
const taskDraft = ref('')

const runs = [
  { id: 'bootstrap', title: 'Project bootstrap', meta: 'verified', active: true },
  { id: 'agent-core', title: 'Agent core', meta: 'next', active: false },
  { id: 'tools', title: 'Local tools', meta: 'planned', active: false },
]

const timeline = [
  { kind: 'system', title: 'Repository initialized', detail: 'Git history started on main.' },
  { kind: 'tool', title: 'Backend scaffold', detail: 'Spring Boot health endpoint verified.' },
  { kind: 'tool', title: 'Frontend scaffold', detail: 'Vue build verified with Vite.' },
]

const files = [
  'frontend/src/App.vue',
  'frontend/src/api/health.ts',
  'backend/src/main/java/com/zhumeiyuan/codingagent/health/HealthController.java',
  'decisions/0004-local-workbench-ui.md',
]

const checks = [
  { name: 'mvn test', result: 'passed' },
  { name: 'npm run build', result: 'passed' },
  { name: 'Vite proxy /api/health', result: 'passed' },
]

const statusLabel = computed(() => {
  if (loading.value) return 'checking'
  if (error.value) return 'offline'
  return health.value?.status ?? 'unknown'
})

onMounted(async () => {
  try {
    health.value = await fetchHealth()
  } catch (caught) {
    error.value = caught instanceof Error ? caught.message : 'Unknown error'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="app-shell">
    <aside class="rail" aria-label="Runs">
      <header class="brand">
        <div class="brand-mark">CA</div>
        <div>
          <p class="eyebrow">Coding Agent</p>
          <h1>Workbench</h1>
        </div>
      </header>

      <nav class="run-list" aria-label="Run history">
        <button
          v-for="run in runs"
          :key="run.id"
          class="run-item"
          :class="{ active: run.active }"
          type="button"
        >
          <span>{{ run.title }}</span>
          <small>{{ run.meta }}</small>
        </button>
      </nav>
    </aside>

    <section class="main-pane" aria-label="Task workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Local workspace</p>
          <h2>/Users/zhumeiyuan/Desktop/CodingAgent</h2>
        </div>
        <span class="status-pill" :class="{ ready: health?.status === 'ok', pending: loading, error }">
          {{ statusLabel }}
        </span>
      </header>

      <section class="thread">
        <article class="message user-message">
          <p class="message-role">User</p>
          <p>Scaffold Vue 3 and Spring Boot, then keep the interface simple, clear, and Codex-like.</p>
        </article>

        <article class="message assistant-message">
          <p class="message-role">Assistant</p>
          <p>Repository baseline is ready. The next implementation layer is the local workspace runner and event stream.</p>
        </article>

        <ol class="timeline" aria-label="Run events">
          <li v-for="item in timeline" :key="item.title">
            <span class="event-kind">{{ item.kind }}</span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.detail }}</p>
            </div>
          </li>
        </ol>

        <section class="health-panel" aria-label="Backend health">
          <p v-if="loading">Checking backend...</p>
          <p v-else-if="error" class="error-text">{{ error }}</p>
          <dl v-else-if="health" class="health-grid">
            <div>
              <dt>Service</dt>
              <dd>{{ health.service }}</dd>
            </div>
            <div>
              <dt>Java</dt>
              <dd>{{ health.javaVersion }}</dd>
            </div>
            <div>
              <dt>Server time</dt>
              <dd>{{ new Date(health.serverTime).toLocaleString() }}</dd>
            </div>
          </dl>
        </section>
      </section>

      <form class="composer" aria-label="Task composer">
        <textarea v-model="taskDraft" aria-label="Task input" placeholder="Ask the agent to change this workspace" rows="2" />
        <button type="button" disabled>Run</button>
      </form>
    </section>

    <aside class="detail-pane" aria-label="Workspace details">
      <div class="tabs" role="tablist" aria-label="Detail tabs">
        <button :class="{ active: detailTab === 'files' }" type="button" @click="detailTab = 'files'">
          Files
        </button>
        <button :class="{ active: detailTab === 'diff' }" type="button" @click="detailTab = 'diff'">
          Diff
        </button>
        <button :class="{ active: detailTab === 'checks' }" type="button" @click="detailTab = 'checks'">
          Checks
        </button>
      </div>

      <section v-if="detailTab === 'files'" class="tab-body">
        <button v-for="file in files" :key="file" class="file-row" type="button">
          {{ file }}
        </button>
      </section>

      <section v-else-if="detailTab === 'diff'" class="tab-body diff-body">
        <p>+ Vue workbench shell</p>
        <p>+ Spring Boot health endpoint</p>
        <p>+ Traceable framework decision</p>
      </section>

      <section v-else class="tab-body">
        <div v-for="check in checks" :key="check.name" class="check-row">
          <span>{{ check.name }}</span>
          <strong>{{ check.result }}</strong>
        </div>
      </section>
    </aside>
  </main>
</template>
