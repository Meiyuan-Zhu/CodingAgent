<script setup lang="ts">
defineProps<{
  name:
    | 'approve'
    | 'chevron-left'
    | 'chevron-right'
    | 'command'
    | 'diff'
    | 'file'
    | 'folder'
    | 'plus'
    | 'review'
    | 'run'
    | 'search'
    | 'stop'
    | 'undo'
    | 'arrow-up'
}>()
</script>

<template>
  <span class="ui-icon" :class="`ui-icon-${name}`" aria-hidden="true">
    <span></span>
  </span>
</template>
