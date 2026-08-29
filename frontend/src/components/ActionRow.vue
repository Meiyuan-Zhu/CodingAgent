<script setup lang="ts">
import { computed } from 'vue'
import { commandResult, type ToolCard } from '../run/toolCards'
import { actionText, statusTone } from '../run/display'

const props = defineProps<{
  card: ToolCard
  selected: boolean
}>()

const emit = defineEmits<{
  select: [toolCallId: string]
}>()

const command = computed(() => commandResult(props.card))
const tone = computed(() => statusTone(props.card.status))
const icon = computed(() => props.card.name === 'run_command' ? '▸' : props.card.name === 'read_file' ? '◇' : props.card.name === 'list_files' ? '▢' : '⌕')
</script>

<template>
  <button class="action-row" :class="[`tone-${tone}`, { selected: props.selected }]" type="button" @click="emit('select', props.card.id)">
    <span class="action-icon">{{ icon }}</span>
    <span class="action-copy">{{ actionText(props.card) }}</span>
    <span v-if="props.card.name === 'run_command' && command" class="action-meta">exit {{ command.exitCode ?? '—' }}</span>
    <span v-else-if="props.card.status === 'running'" class="action-meta live-dot">live</span>
  </button>
</template>
