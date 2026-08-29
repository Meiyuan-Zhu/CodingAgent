<script setup lang="ts">
import { computed } from 'vue'
import { type ToolCard } from '../run/toolCards'
import { changeStats, changeTitle, statusTone, toolPath } from '../run/display'

const props = defineProps<{
  card: ToolCard
  selected: boolean
}>()

const emit = defineEmits<{
  select: [toolCallId: string]
}>()

const stats = computed(() => changeStats(props.card))
const path = computed(() => toolPath(props.card) ?? 'workspace file')
const tone = computed(() => statusTone(props.card.status))
</script>

<template>
  <article class="change-summary-card" :class="[`tone-${tone}`, { selected: props.selected }]">
    <div class="change-icon" aria-hidden="true">⊞</div>
    <button class="change-main" type="button" @click="emit('select', props.card.id)">
      <strong>{{ changeTitle(props.card) }}</strong>
      <small>{{ path }}</small>
      <span class="change-stats"><b>+{{ stats.additions }}</b> <i>-{{ stats.deletions }}</i></span>
    </button>
    <button class="review-button" type="button" @click="emit('select', props.card.id)">审查</button>
  </article>
</template>
