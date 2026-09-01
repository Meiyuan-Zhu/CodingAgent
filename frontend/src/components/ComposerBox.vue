<script setup lang="ts">
import UiIcon from './UiIcon.vue'

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

const suggestions = [
  {
    label: '修复 demo 测试',
    prompt: '修复 demo workspace 中失败的 Python pricing 测试，并运行 unittest 验证。',
  },
  {
    label: '审查最近改动',
    prompt: '阅读当前 repo 的最近改动，指出可能的风险、遗漏的测试和需要我确认的地方。',
  },
  {
    label: '优化前端体验',
    prompt: '检查前端工作台界面，按 Codex-like 方向优化交互、文案和视觉细节。',
  },
]

function handleEnter(event: KeyboardEvent) {
  if (event.isComposing || event.shiftKey) return
  event.preventDefault()
  emit('submit')
}

function useSuggestion(prompt: string) {
  model.value = prompt
}
</script>

<template>
  <form class="composer-box" aria-label="任务输入" novalidate @submit.prevent="emit('submit')">
    <textarea
      v-model="model"
      aria-label="任务内容"
      class="resize-none"
      placeholder="描述你希望 Agent 完成的修改、检查或验证"
      rows="3"
      @keydown.enter="handleEnter"
      @keydown.meta.enter.prevent="emit('submit')"
      @keydown.ctrl.enter.prevent="emit('submit')"
    />
    <div v-if="model.trim().length === 0" class="composer-suggestions" aria-label="快捷任务建议">
      <button v-for="suggestion in suggestions" :key="suggestion.label" type="button" @click="useSuggestion(suggestion.prompt)">
        {{ suggestion.label }}
      </button>
    </div>
    <footer class="composer-footer">
      <div class="composer-hints">
        <span>本地 workspace</span>
        <span>编辑和命令需要批准</span>
        <span>Shift Return 换行</span>
      </div>
      <div class="composer-actions">
        <button class="ghost-button" type="button" :disabled="!props.canCancel" aria-label="停止当前任务" @click="emit('cancel')">
          <UiIcon name="stop" />
          {{ props.cancelling ? '停止中' : '停止' }}
        </button>
        <button class="send-button" type="submit" :disabled="props.disabled">
          <UiIcon name="run" />
          {{ props.submitting ? '启动中' : '运行' }}
        </button>
      </div>
    </footer>
  </form>
</template>
