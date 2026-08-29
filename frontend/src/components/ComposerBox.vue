<script setup lang="ts">
const model = defineModel<string>({ required: true })

const props = defineProps<{
  disabled: boolean
  submitting: boolean
  cancelling: boolean
  canCancel: boolean
}>()

const emit = defineEmits<{
  submit: []
  cancel: []
}>()
</script>

<template>
  <form class="composer-box" aria-label="Task composer" novalidate @submit.prevent="emit('submit')">
    <textarea
      v-model="model"
      aria-label="Task input"
      class="resize-none"
      placeholder="Ask the agent to inspect, edit, and verify this workspace"
      rows="3"
      @keydown.enter.exact.prevent="emit('submit')"
      @keydown.meta.enter.prevent="emit('submit')"
      @keydown.ctrl.enter.prevent="emit('submit')"
    />
    <footer class="composer-footer">
      <div class="composer-hints">
        <span>Local workspace</span>
        <span>Tools need approval for edits and commands</span>
      </div>
      <div class="composer-actions">
        <button class="ghost-button" type="button" :disabled="!props.canCancel" @click="emit('cancel')">
          {{ props.cancelling ? 'Cancelling' : 'Cancel' }}
        </button>
        <button class="send-button" type="submit" :disabled="props.disabled">
          {{ props.submitting ? 'Starting' : 'Run' }}
        </button>
      </div>
    </footer>
  </form>
</template>
