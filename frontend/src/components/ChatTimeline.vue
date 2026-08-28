<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import ApprovalCard from './ApprovalCard.vue'
import ToolCallCard from './ToolCallCard.vue'
import type { ApprovalView, TimelineItem } from '../run/timeline'

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
        <h2>Start a coding task</h2>
        <p>Ask the agent to inspect files, make a small change, and verify it with a command. Tool actions will appear as cards, and risky actions will ask for permission.</p>
      </div>
    </article>

    <template v-for="item in props.items" :key="item.id">
      <article v-if="item.kind === 'user'" class="message-row user-row">
        <div class="message-bubble user-bubble">
          <p>{{ item.content }}</p>
        </div>
      </article>

      <article v-else-if="item.kind === 'assistant'" class="message-row assistant-row">
        <div class="assistant-avatar">CA</div>
        <div class="message-bubble assistant-bubble">
          <header>
            <span>{{ item.title }}</span>
            <em v-if="item.streaming">live</em>
          </header>
          <p>{{ item.content }}</p>
        </div>
      </article>

      <article v-else-if="item.kind === 'approval'" class="message-row assistant-row">
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

      <article v-else-if="item.kind === 'tool'" class="message-row assistant-row compact-row">
        <div class="assistant-avatar tool-avatar">⌘</div>
        <ToolCallCard :card="item.card" :selected="props.selectedToolCallId === item.card.id" @select="emit('selectTool', $event)" />
      </article>

      <article v-else class="message-row assistant-row compact-row">
        <div class="assistant-avatar done-avatar">✓</div>
        <div class="message-bubble run-bubble" :class="`run-${item.status.toLowerCase()}`">
          <header>
            <span>{{ item.title }}</span>
          </header>
          <p>{{ item.content }}</p>
        </div>
      </article>
    </template>

    <p v-if="props.runError" class="run-error">{{ props.runError }}</p>
  </section>
</template>
