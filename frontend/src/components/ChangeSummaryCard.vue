<script setup lang="ts">
import { computed } from 'vue'
import { type ToolCard } from '../run/toolCards'
import { changeStats, changeTitle, statusTone, toolPath } from '../run/display'
import UiIcon from './UiIcon.vue'

const props = defineProps<{
  card: ToolCard
  selected: boolean
  undoing: boolean
}>()

const emit = defineEmits<{
  select: [toolCallId: string]
  undo: [toolCallId: string]
}>()

const stats = computed(() => changeStats(props.card))
const path = computed(() => toolPath(props.card) ?? 'workspace file')
const tone = computed(() => statusTone(props.card.status))
</script>

<template>
  <article class="change-summary-card" :class="[`tone-${tone}`, { selected: props.selected, undone: props.card.undone }]">
    <div class="change-icon" aria-hidden="true"><UiIcon :name="props.card.undone ? 'undo' : 'diff'" /></div>
    <button class="change-main" type="button" @click="emit('select', props.card.id)">
      <strong>{{ changeTitle(props.card) }}</strong>
      <small>{{ path }}</small>
      <span class="change-stats"><b>+{{ stats.additions }}</b> <i>-{{ stats.deletions }}</i></span>
    </button>
    <div class="change-actions">
      <button class="review-button" type="button" @click="emit('select', props.card.id)">审查</button>
      <button
        v-if="props.card.undoable || props.card.undone"
        class="undo-button"
        type="button"
        :disabled="props.card.undone || props.undoing"
        :aria-label="props.card.undone ? '变更已撤销' : `撤销 ${path}`"
        @click="emit('undo', props.card.id)"
      >
        <UiIcon name="undo" />
        {{ props.card.undone ? '已撤销' : props.undoing ? '撤销中' : '撤销' }}
      </button>
    </div>
  </article>
</template>
