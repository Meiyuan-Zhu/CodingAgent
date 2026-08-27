<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchHealth, type HealthResponse } from './api/health'

const health = ref<HealthResponse | null>(null)
const loading = ref(true)
const error = ref<string | null>(null)

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
  <main class="shell">
    <section class="workspace">
      <header class="topbar">
        <div>
          <p class="eyebrow">Coding Agent</p>
          <h1>Local agent workbench</h1>
        </div>
        <span class="status-pill" :class="{ ready: health?.status === 'ok', pending: loading, error }">
          {{ loading ? 'checking' : error ? 'offline' : health?.status }}
        </span>
      </header>

      <section class="layout">
        <aside class="sidebar" aria-label="Run list">
          <p class="panel-label">Runs</p>
          <button class="run-item active" type="button">
            <span class="run-dot"></span>
            Project bootstrap
          </button>
        </aside>

        <section class="conversation" aria-label="Agent task">
          <div class="prompt-box">
            <p class="panel-label">Task</p>
            <p>Scaffold Vue 3 and Spring Boot as separate applications.</p>
          </div>

          <div class="event-card">
            <p class="panel-label">Backend connection</p>
            <p v-if="loading">Waiting for Spring Boot...</p>
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
          </div>
        </section>

        <aside class="detail" aria-label="Details">
          <p class="panel-label">Next module</p>
          <p>Agent loop, tool registry, event stream, and run storage will attach behind this shell.</p>
        </aside>
      </section>
    </section>
  </main>
</template>
