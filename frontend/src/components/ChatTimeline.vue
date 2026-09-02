<script setup lang="ts">
import { nextTick, onUnmounted, ref, watch } from 'vue'
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
  undoingToolCallId: string | null
  runError: string | null
}>()

const emit = defineEmits<{
  selectTool: [toolCallId: string]
  undoChange: [toolCallId: string]
  approve: []
  reject: []
  copyUserMessage: [content: string]
  editUserMessage: [content: string]
}>()

const timelineElement = ref<HTMLElement | null>(null)
const displayedAssistantContent = ref<Record<string, string>>({})
const userPinnedToBottom = ref(true)
const streamTimers = new Map<string, number>()

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
    if (!item.streaming) {
      stopStreaming(item.id)
      displayedAssistantContent.value[item.id] = item.content
      continue
    }
    if (current === item.content) continue
    if (streamTimers.has(item.id)) {
      stopStreaming(item.id)
    }
    startStreaming(item.id, item.content, current)
  }
}, { immediate: true })

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
  if (!timelineElement.value || !userPinnedToBottom.value) return
  timelineElement.value.scrollTop = timelineElement.value.scrollHeight
})

function handleScroll() {
  if (!timelineElement.value) return
  const distanceFromBottom = timelineElement.value.scrollHeight - timelineElement.value.scrollTop - timelineElement.value.clientHeight
  userPinnedToBottom.value = distanceFromBottom < 96
}
</script>

<template>
  <section ref="timelineElement" class="chat-timeline" aria-label="Agent 对话" @scroll="handleScroll">
    <article v-if="props.items.length === 0" class="empty-thread">
      <div class="assistant-avatar">CA</div>
      <div>
        <h2>要让 Agent 改什么？</h2>
        <p>描述你希望 Agent 在本地 workspace 中完成的任务。它会读取文件、提出修改、请求权限，并把可审查的变更展示在右侧。</p>
      </div>
    </article>

    <template v-for="item in props.items" :key="item.id">
      <article v-if="item.kind === 'user'" class="message-row user-row">
        <div class="user-message-stack">
          <div class="message-bubble user-bubble">
            <p>{{ item.content }}</p>
          </div>
          <div class="message-actions user-message-actions" aria-label="用户消息操作">
            <button type="button" aria-label="复制用户消息" title="复制" @click="emit('copyUserMessage', item.content)">
              复制
            </button>
            <button type="button" aria-label="修改用户消息" title="修改" @click="emit('editUserMessage', item.content)">
              修改
            </button>
          </div>
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
        <ChangeSummaryCard
          :card="item.card"
          :selected="props.selectedToolCallId === item.card.id"
          :undoing="props.undoingToolCallId === item.card.id"
          @select="emit('selectTool', $event)"
          @undo="emit('undoChange', $event)"
        />
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

    <p v-if="props.runError" class="run-error">{{ props.runError }}</p>
  </section>
</template>
