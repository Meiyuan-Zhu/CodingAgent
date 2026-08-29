<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import ActionRow from './ActionRow.vue'
import ApprovalCard from './ApprovalCard.vue'
import ChangeSummaryCard from './ChangeSummaryCard.vue'
import MarkdownBlock from './MarkdownBlock.vue'
import type { ApprovalView, TimelineItem } from '../run/timeline'
import { isChangeTool } from '../run/display'

const props = defineProps<{
  items: TimelineItem[]
  pendingApproval: ApprovalView | null
  selectedToolCallId: string | null
  resolvingApproval: boolean
  runError: string | null
}>()

const emit = defineEmits<{
  selectTool: [toolCallId: string]
  approve: []
  reject: []
}>()

const timelineElement = ref<HTMLElement | null>(null)
const displayedAssistantContent = ref<Record<string, string>>({})
const streamTimers = new Map<string, number>()

const changedCount = computed(() => props.items.filter((item) => item.kind === 'tool' && isChangeTool(item.card) && item.card.status === 'finished').length)
const firstChangedToolId = computed(() => {
  const item = props.items.find((entry) => entry.kind === 'tool' && isChangeTool(entry.card))
  return item?.kind === 'tool' ? item.card.id : ''
})

const changeTotals = computed(() => {
  let additions = 0
  let deletions = 0
  for (const item of props.items) {
    if (item.kind !== 'tool' || !isChangeTool(item.card) || !item.card.result) continue
    const diff = typeof item.card.result.unifiedDiff === 'string' ? item.card.result.unifiedDiff : ''
    for (const line of diff.split('\n')) {
      if (line.startsWith('+++') || line.startsWith('---')) continue
      if (line.startsWith('+')) additions += 1
      if (line.startsWith('-')) deletions += 1
    }
  }
  return { additions, deletions }
})


watch(() => props.items, (items) => {
  const assistantIds = new Set(items.filter((item) => item.kind === 'assistant').map((item) => item.id))
  for (const key of Object.keys(displayedAssistantContent.value)) {
    if (!assistantIds.has(key)) {
      delete displayedAssistantContent.value[key]
      stopStreaming(key)
    }
  }

  for (const item of items) {
    if (item.kind !== 'assistant') continue
    const current = displayedAssistantContent.value[item.id] ?? ''
    if (current === item.content) continue
    if (streamTimers.has(item.id)) continue
    if (!item.streaming) {
      displayedAssistantContent.value[item.id] = item.content
      continue
    }
    startStreaming(item.id, item.content, current)
  }
}, { immediate: true, deep: true })

onUnmounted(() => {
  for (const id of streamTimers.keys()) stopStreaming(id)
})

function assistantContent(item: Extract<TimelineItem, { kind: 'assistant' }>) {
  return displayedAssistantContent.value[item.id] ?? item.content
}

function startStreaming(id: string, fullContent: string, currentContent: string) {
  let cursor = Math.min(currentContent.length, fullContent.length)
  displayedAssistantContent.value[id] = fullContent.slice(0, cursor)
  const timer = window.setInterval(() => {
    cursor = Math.min(fullContent.length, cursor + streamChunkSize(fullContent, cursor))
    displayedAssistantContent.value[id] = fullContent.slice(0, cursor)
    if (cursor >= fullContent.length) stopStreaming(id)
  }, 18)
  streamTimers.set(id, timer)
}

function stopStreaming(id: string) {
  const timer = streamTimers.get(id)
  if (timer !== undefined) window.clearInterval(timer)
  streamTimers.delete(id)
}

function streamChunkSize(content: string, cursor: number) {
  const next = content.slice(cursor, cursor + 12)
  if (next.startsWith('```')) return 12
  if (/^[\s\n]+$/.test(next)) return 6
  return content.length > 600 ? 8 : 3
}

watch(() => props.items.length, async () => {
  await nextTick()
  if (!timelineElement.value) return
  timelineElement.value.scrollTop = timelineElement.value.scrollHeight
})
</script>

<template>
  <section ref="timelineElement" class="chat-timeline" aria-label="Agent conversation">
    <article v-if="props.items.length === 0" class="empty-thread">
      <div class="assistant-avatar">CA</div>
      <div>
        <h2>开始一个 coding task</h2>
        <p>描述你希望 Agent 在本地 workspace 中完成的任务。它会读取文件、提出修改、请求权限，并把可审查的变更展示在右侧。</p>
      </div>
    </article>

    <template v-for="item in props.items" :key="item.id">
      <article v-if="item.kind === 'user'" class="message-row user-row">
        <div class="message-bubble user-bubble">
          <p>{{ item.content }}</p>
        </div>
      </article>

      <article v-else-if="item.kind === 'thinking'" class="message-row assistant-row status-row">
        <div class="thinking-copy"><span></span>{{ item.content }}</div>
      </article>

      <article v-else-if="item.kind === 'assistant'" class="message-row assistant-row narrative-row">
        <div class="message-bubble assistant-bubble narrative-bubble">
          <MarkdownBlock :content="assistantContent(item)" />
          <span v-if="item.streaming && assistantContent(item).length < item.content.length" class="stream-cursor" aria-hidden="true"></span>
        </div>
      </article>

      <article v-else-if="item.kind === 'approval'" class="message-row assistant-row approval-row">
        <div class="assistant-avatar permission-avatar">!</div>
        <ApprovalCard
          :approval="props.pendingApproval && props.pendingApproval.toolCallId === item.card.id ? props.pendingApproval : {
            toolCallId: item.card.id,
            name: item.card.name,
            arguments: item.card.arguments,
            reason: item.reason,
            risk: item.reason,
            diff: null,
            command: null,
            cwd: null,
          }"
          :resolving="props.resolvingApproval"
          @approve="emit('approve')"
          @reject="emit('reject')"
        />
      </article>

      <article v-else-if="item.kind === 'tool' && isChangeTool(item.card)" class="message-row assistant-row change-row">
        <ChangeSummaryCard :card="item.card" :selected="props.selectedToolCallId === item.card.id" @select="emit('selectTool', $event)" />
      </article>

      <article v-else-if="item.kind === 'tool'" class="message-row assistant-row action-row-shell">
        <ActionRow :card="item.card" :selected="props.selectedToolCallId === item.card.id" @select="emit('selectTool', $event)" />
      </article>

      <article v-else class="message-row assistant-row status-row">
        <div class="run-status-line" :class="`run-${item.status.toLowerCase()}`">
          <span>{{ item.title }}</span>
          <small>{{ item.content }}</small>
        </div>
      </article>
    </template>

    <button v-if="changedCount > 0" class="floating-change-chip" type="button" @click="emit('selectTool', firstChangedToolId)">
      {{ changedCount }} 个文件已更改 <b>+{{ changeTotals.additions }}</b> <i>-{{ changeTotals.deletions }}</i>
    </button>

    <p v-if="props.runError" class="run-error">{{ props.runError }}</p>
  </section>
</template>
