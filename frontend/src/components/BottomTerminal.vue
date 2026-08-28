<script setup lang="ts">
import type { TerminalSession } from '../run/timeline'

const props = defineProps<{
  terminal: TerminalSession | null
}>()
</script>

<template>
  <section class="bottom-terminal" aria-label="Command output terminal">
    <header>
      <div>
        <p class="section-label">Terminal</p>
        <h2>{{ props.terminal?.title ?? 'No command yet' }}</h2>
      </div>
      <span v-if="props.terminal" class="terminal-status">{{ props.terminal.status }} · exit {{ props.terminal.exitCode ?? '—' }}</span>
    </header>
    <div v-if="props.terminal" class="terminal-screen">
      <p><span>$</span> {{ props.terminal.command }}</p>
      <p class="terminal-cwd">cwd: {{ props.terminal.cwd }} · duration: {{ props.terminal.duration }}</p>
      <pre v-if="props.terminal.stdout !== '∅'">{{ props.terminal.stdout }}</pre>
      <pre v-if="props.terminal.stderr !== '∅'" class="stderr">{{ props.terminal.stderr }}</pre>
      <pre v-if="props.terminal.stdout === '∅' && props.terminal.stderr === '∅'">∅</pre>
    </div>
    <p v-else class="empty-copy">When the agent asks to run tests or another command, output will stream into this area after approval.</p>
  </section>
</template>
