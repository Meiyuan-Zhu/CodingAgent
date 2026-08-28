<script setup lang="ts">
import { computed } from 'vue'
import { commandCwd, commandLine, commandResult, formatOccurredAt, outputText, toolStatusLabel, type ToolCard } from '../run/toolCards'
import { diffPreview } from '../run/timeline'

const props = defineProps<{
  card: ToolCard
  selected: boolean
}>()

const emit = defineEmits<{
  select: [toolCallId: string]
}>()

const diff = computed(() => diffPreview(props.card))
const command = computed(() => commandResult(props.card))
</script>

<template>
  <button class="tool-call-card" :class="[`tool-state-${props.card.status}`, { selected: props.selected }]" type="button" @click="emit('select', props.card.id)">
    <header>
      <span class="tool-icon">⌘</span>
      <span>
        <strong>{{ props.card.name }}</strong>
        <small>{{ toolStatusLabel(props.card.status) }} · {{ formatOccurredAt(props.card.requestedAt) }}</small>
      </span>
      <em>{{ toolStatusLabel(props.card.status) }}</em>
    </header>

    <p v-if="props.card.name === 'run_command'" class="tool-primary"><code>{{ commandLine(props.card) }}</code></p>
    <p v-else-if="diff" class="tool-primary">Diff ready for {{ diff.path }}</p>
    <p v-else class="tool-primary">{{ props.card.rawContent ? 'Result available' : 'Waiting for result' }}</p>

    <div v-if="props.card.name === 'run_command'" class="tool-facts">
      <span>cwd {{ commandCwd(props.card) }}</span>
      <span>exit {{ command?.exitCode ?? '—' }}</span>
    </div>
    <p v-if="props.card.error" class="inline-error">{{ props.card.error }}</p>
    <p v-else-if="props.card.name === 'run_command' && command" class="command-peek">{{ outputText(command.stderr || command.stdout).slice(0, 180) }}</p>
  </button>
</template>
