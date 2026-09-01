<script setup lang="ts">
import ActionRow from './ActionRow.vue'
import ChangeSummaryCard from './ChangeSummaryCard.vue'
import type { ToolCard } from '../run/toolCards'
import { isChangeTool } from '../run/display'

const props = defineProps<{
  card: ToolCard
  selected: boolean
  undoing: boolean
}>()

const emit = defineEmits<{
  select: [toolCallId: string]
  undo: [toolCallId: string]
}>()
</script>

<template>
  <ChangeSummaryCard
    v-if="isChangeTool(props.card)"
    :card="props.card"
    :selected="props.selected"
    :undoing="props.undoing"
    @select="emit('select', $event)"
    @undo="emit('undo', $event)"
  />
  <ActionRow v-else :card="props.card" :selected="props.selected" @select="emit('select', $event)" />
</template>
