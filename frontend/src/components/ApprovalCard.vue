<script setup lang="ts">
import { computed } from 'vue'
import type { ApprovalView } from '../run/timeline'
import { approvalActionTitle, argumentContent, argumentPath, diffStats } from '../run/display'

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
const targetPath = computed(() => props.approval.diff?.path ?? argumentPath(props.approval.arguments))
const contentPreview = computed(() => argumentContent(props.approval.arguments))
const title = computed(() => approvalActionTitle(props.approval.name, props.approval.arguments, targetPath.value))
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
        <p class="mini-label">拟修改文件</p>
        <strong>{{ props.approval.diff.path }}</strong>
      </div>
      <span class="change-stats"><b>+{{ stats.additions }}</b> <i>-{{ stats.deletions }}</i></span>
    </section>

    <section v-else-if="targetPath" class="approval-diff-preview">
      <div>
        <p class="mini-label">拟写入文件</p>
        <strong>{{ targetPath }}</strong>
      </div>
    </section>

    <section v-if="contentPreview" class="approval-content-preview">
      <p class="mini-label">拟写入内容</p>
      <pre>{{ contentPreview }}</pre>
    </section>

    <details v-if="!props.approval.command && !targetPath && !contentPreview" class="raw-details">
      <summary>查看详情</summary>
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
