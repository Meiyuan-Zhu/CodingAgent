<script setup lang="ts">
import { ref } from 'vue'
import UiIcon from './UiIcon.vue'

const model = defineModel<string>({ required: true })

const props = defineProps<{
  disabled: boolean
  submitting: boolean
  cancelling: boolean
  canCancel: boolean
  autoApprove: boolean
}>()

const emit = defineEmits<{
  submit: []
  cancel: []
  setAutoApprove: [enabled: boolean]
}>()

const approvalMenuOpen = ref(false)

function handleEnter(event: KeyboardEvent) {
  if (event.isComposing || event.shiftKey) return
  event.preventDefault()
  emit('submit')
}

function selectApprovalMode(enabled: boolean) {
  emit('setAutoApprove', enabled)
  approvalMenuOpen.value = false
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
    <footer class="composer-footer">
      <div class="composer-hints">
        <div class="approval-mode" @keydown.escape.stop="approvalMenuOpen = false">
          <button
            class="approval-mode-button"
            type="button"
            :class="{ active: props.autoApprove }"
            :aria-expanded="approvalMenuOpen"
            aria-haspopup="menu"
            aria-label="选择批准方式"
            @click="approvalMenuOpen = !approvalMenuOpen"
          >
            <span>{{ props.autoApprove ? '帮我批准' : '请求批准' }}</span>
            <span class="approval-caret" aria-hidden="true"></span>
          </button>

          <div v-if="approvalMenuOpen" class="approval-mode-menu" role="menu" aria-label="批准方式">
            <button type="button" role="menuitemradio" :aria-checked="!props.autoApprove" @click="selectApprovalMode(false)">
              <span>
                <strong>请求批准</strong>
                <small>修改文件和执行命令前询问</small>
              </span>
            </button>
            <button type="button" role="menuitemradio" :aria-checked="props.autoApprove" @click="selectApprovalMode(true)">
              <span>
                <strong>帮我批准</strong>
                <small>自动批准检测到的操作</small>
              </span>
            </button>
          </div>
        </div>
      </div>
      <div class="composer-actions">
        <button v-if="props.canCancel" class="ghost-button" type="button" aria-label="停止当前任务" @click="emit('cancel')">
          <UiIcon name="stop" />
          <span>{{ props.cancelling ? '停止中' : '停止' }}</span>
        </button>
        <button class="send-button" type="submit" :disabled="props.disabled" :aria-label="props.submitting ? '启动中' : '运行'">
          <span class="send-arrow" aria-hidden="true">↑</span>
        </button>
      </div>
    </footer>
  </form>
</template>
