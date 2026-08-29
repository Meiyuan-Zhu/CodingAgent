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
      placeholder="描述你希望 Agent 完成的修改、检查或验证"
      rows="3"
      @keydown.enter.exact.prevent="emit('submit')"
      @keydown.meta.enter.prevent="emit('submit')"
      @keydown.ctrl.enter.prevent="emit('submit')"
    />
    <footer class="composer-footer">
      <div class="composer-hints">
        <span>本地 workspace</span>
        <span>编辑和命令需要批准</span>
      </div>
      <div class="composer-actions">
        <button class="ghost-button" type="button" :disabled="!props.canCancel" @click="emit('cancel')">
          {{ props.cancelling ? '取消中' : '取消' }}
        </button>
        <button class="send-button" type="submit" :disabled="props.disabled">
          {{ props.submitting ? '启动中' : '运行' }}
        </button>
      </div>
    </footer>
  </form>
</template>
