<script setup lang="ts">
import { computed } from 'vue'
import type { ApprovalView } from '../run/timeline'

const props = defineProps<{
  approval: ApprovalView
  resolving: boolean
}>()

const emit = defineEmits<{
  approve: []
  reject: []
}>()

const formattedArguments = computed(() => JSON.stringify(props.approval.arguments ?? {}, null, 2))
</script>

<template>
  <article class="approval-card">
    <header>
      <p class="label">Permission request</p>
      <h3>{{ props.approval.name }}</h3>
    </header>

    <p class="risk-copy">{{ props.approval.risk }}</p>

    <section v-if="props.approval.command" class="approval-command">
      <p class="mini-label">Command</p>
      <code>{{ props.approval.command }}</code>
      <small>cwd: {{ props.approval.cwd ?? '.' }}</small>
    </section>

    <section v-if="props.approval.diff" class="approval-diff">
      <p class="mini-label">Proposed diff · {{ props.approval.diff.path }}</p>
      <pre>{{ props.approval.diff.diff }}</pre>
    </section>

    <details v-else class="raw-details">
      <summary>Arguments</summary>
      <pre>{{ formattedArguments }}</pre>
    </details>

    <footer class="approval-actions">
      <button class="approve-button" type="button" :disabled="props.resolving" @click="emit('approve')">
        {{ props.resolving ? 'Resolving' : 'Approve' }}
      </button>
      <button class="reject-button" type="button" :disabled="props.resolving" @click="emit('reject')">Reject</button>
    </footer>
  </article>
</template>
