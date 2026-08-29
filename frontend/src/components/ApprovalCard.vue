<script setup lang="ts">
import { computed } from 'vue'
import type { ApprovalView } from '../run/timeline'
import { basename, diffStats } from '../run/display'

const props = defineProps<{
  approval: ApprovalView
  resolving: boolean
}>()

const emit = defineEmits<{
  approve: []
  reject: []
}>()

const formattedArguments = computed(() => JSON.stringify(props.approval.arguments ?? {}, null, 2))
const stats = computed(() => diffStats(props.approval.diff?.diff))
const targetName = computed(() => basename(props.approval.diff?.path ?? null))
const title = computed(() => {
  if (props.approval.command) return 'Agent 想运行命令'
  if (props.approval.diff) return `Agent 想编辑 ${targetName.value}`
  return `Agent 请求使用 ${props.approval.name}`
})
</script>

<template>
  <article class="approval-card">
    <header>
      <p class="label">权限请求</p>
      <h3>{{ title }}</h3>
    </header>

    <p class="risk-copy">{{ props.approval.risk }}</p>

    <section v-if="props.approval.command" class="approval-command">
      <p class="mini-label">Command</p>
      <code>{{ props.approval.command }}</code>
      <small>cwd: {{ props.approval.cwd ?? '.' }}</small>
    </section>

    <section v-if="props.approval.diff" class="approval-diff-preview">
      <div>
        <p class="mini-label">Proposed change</p>
        <strong>{{ props.approval.diff.path }}</strong>
      </div>
      <span class="change-stats"><b>+{{ stats.additions }}</b> <i>-{{ stats.deletions }}</i></span>
    </section>

    <details v-if="!props.approval.command && !props.approval.diff" class="raw-details">
      <summary>查看参数</summary>
      <pre>{{ formattedArguments }}</pre>
    </details>

    <footer class="approval-actions">
      <button class="approve-button" type="button" :disabled="props.resolving" @click="emit('approve')">
        {{ props.resolving ? '处理中' : '批准' }}
      </button>
      <button class="reject-button" type="button" :disabled="props.resolving" @click="emit('reject')">拒绝</button>
    </footer>
  </article>
</template>
