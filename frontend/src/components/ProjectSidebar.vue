<script setup lang="ts">
import { ref } from 'vue'
import type { RunResponse } from '../api/runs'
import type { WorkspaceProject } from '../api/workspace'
import UiIcon from './UiIcon.vue'

const props = defineProps<{
  activeRunId: string | null
  runs: RunResponse[]
  runTitles: Record<string, string>
  projects: WorkspaceProject[]
  activeProjectId: string | null
  addingProject: boolean
  choosingProjectFolder: boolean
  deletingRunId: string | null
}>()

const emit = defineEmits<{
  selectRun: [run: RunResponse]
  newTask: []
  addProject: [path: string, create: boolean]
  chooseProjectFolder: []
  selectProject: [project: WorkspaceProject]
  deleteRun: [run: RunResponse]
}>()

const addingProjectOpen = ref(false)
const projectPath = ref('')
const createProjectDirectory = ref(false)
const manualProjectPathOpen = ref(false)

function shortId(id: string) {
  return id.slice(0, 8)
}

function titleFor(run: RunResponse) {
  return props.runTitles[run.id] || shortId(run.id)
}

function submitProject() {
  const path = projectPath.value.trim()
  if (!path || props.addingProject) return
  emit('addProject', path, createProjectDirectory.value)
  projectPath.value = ''
  createProjectDirectory.value = false
  manualProjectPathOpen.value = false
  addingProjectOpen.value = false
}

function chooseProjectFolder() {
  if (props.addingProject || props.choosingProjectFolder) return
  emit('chooseProjectFolder')
  manualProjectPathOpen.value = false
  addingProjectOpen.value = false
}
</script>

<template>
  <aside class="sidebar" aria-label="项目和任务">
    <header class="sidebar-brand">
      <div class="brand-dot">CA</div>
      <div>
        <div class="brand-title">Coding Agent</div>
      </div>
    </header>

    <button class="new-task-button" type="button" @click="emit('newTask')">
      <UiIcon name="plus" />
      新任务
    </button>

    <section class="sidebar-section">
      <p class="section-label">项目</p>
      <button
        v-for="project in props.projects"
        :key="project.id"
        class="project-row"
        :class="{ active: props.activeProjectId === project.id }"
        type="button"
        :title="project.path"
        @click="emit('selectProject', project)"
      >
        <span class="folder-icon"><UiIcon name="folder" /></span>
        <span>
          <strong>{{ project.name }}</strong>
          <small>{{ project.path }}</small>
        </span>
      </button>
      <button class="project-add-button" type="button" @click="addingProjectOpen = !addingProjectOpen">
        <UiIcon name="plus" />
        添加项目
      </button>
      <div v-if="addingProjectOpen" class="project-add-form">
        <button
          class="project-folder-button"
          type="button"
          :disabled="props.addingProject || props.choosingProjectFolder"
          @click="chooseProjectFolder"
        >
          {{ props.choosingProjectFolder ? '正在打开选择器' : '选择文件夹' }}
        </button>
        <button class="manual-path-toggle" type="button" @click="manualProjectPathOpen = !manualProjectPathOpen">
          {{ manualProjectPathOpen ? '收起手动路径' : '手动输入路径' }}
        </button>
        <form v-if="manualProjectPathOpen" class="manual-project-form" novalidate @submit.prevent="submitProject">
          <input v-model="projectPath" type="text" placeholder="/Users/me/code/my-project" :disabled="props.addingProject">
          <label>
            <input v-model="createProjectDirectory" type="checkbox" :disabled="props.addingProject">
            新建文件夹
          </label>
          <button type="submit" :disabled="props.addingProject || !projectPath.trim()">
            {{ props.addingProject ? '添加中' : '添加' }}
          </button>
        </form>
      </div>
    </section>

    <section class="sidebar-section runs-section">
      <p class="section-label">任务</p>
      <div
        v-for="run in props.runs"
        :key="run.id"
        class="run-row"
        :class="{ active: props.activeRunId === run.id }"
      >
        <button class="run-select-button" type="button" @click="emit('selectRun', run)">
          <span>
            <strong>{{ titleFor(run) }}</strong>
          </span>
        </button>
        <button
          class="run-delete-button"
          type="button"
          :disabled="props.deletingRunId === run.id"
          aria-label="删除任务"
          @click.stop="emit('deleteRun', run)"
        >
          ×
        </button>
      </div>
      <p v-if="props.runs.length === 0" class="empty-copy">还没有任务。</p>
    </section>
  </aside>
</template>
