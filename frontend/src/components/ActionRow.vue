<script setup lang="ts">
import { computed } from 'vue'
import { commandResult, type ToolCard } from '../run/toolCards'
import { actionText, statusTone } from '../run/display'
import UiIcon from './UiIcon.vue'

const props = defineProps<{
  card: ToolCard
  selected: boolean
}>()

const emit = defineEmits<{
  select: [toolCallId: string]
}>()

const command = computed(() => commandResult(props.card))
const tone = computed(() => statusTone(props.card.status))
const iconName = computed(() => props.card.name === 'run_command' ? 'command' : props.card.name === 'read_file' ? 'file' : props.card.name === 'list_files' ? 'folder' : 'search')
</script>

<template>
  <button class="action-row" :class="[`tone-${tone}`, { selected: props.selected }]" type="button" @click="emit('select', props.card.id)">
    <span class="action-icon"><UiIcon :name="iconName" /></span>
    <span class="action-copy">{{ actionText(props.card) }}</span>
    <span v-if="props.card.name === 'run_command' && command" class="action-meta">退出 {{ command.exitCode ?? '—' }}</span>
    <span v-else-if="props.card.status === 'running'" class="action-meta live-dot">运行中</span>
  </button>
</template>
