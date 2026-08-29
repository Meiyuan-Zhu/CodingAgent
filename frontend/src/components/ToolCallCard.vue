<script setup lang="ts">
import ActionRow from './ActionRow.vue'
import ChangeSummaryCard from './ChangeSummaryCard.vue'
import type { ToolCard } from '../run/toolCards'
import { isChangeTool } from '../run/display'

const props = defineProps<{
  card: ToolCard
  selected: boolean
}>()

const emit = defineEmits<{
  select: [toolCallId: string]
}>()
</script>

<template>
  <ChangeSummaryCard v-if="isChangeTool(props.card)" :card="props.card" :selected="props.selected" @select="emit('select', $event)" />
  <ActionRow v-else :card="props.card" :selected="props.selected" @select="emit('select', $event)" />
</template>
