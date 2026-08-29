<script setup lang="ts">
import { computed } from 'vue'

type InlineToken =
  | { type: 'text'; text: string }
  | { type: 'code'; text: string }
  | { type: 'strong'; text: string }

type MarkdownBlock =
  | { type: 'paragraph'; id: string; tokens: InlineToken[] }
  | { type: 'heading'; id: string; level: 2 | 3; tokens: InlineToken[] }
  | { type: 'code'; id: string; language: string; code: string }
  | { type: 'list'; id: string; items: InlineToken[][] }

const props = defineProps<{
  content: string
}>()

const blocks = computed(() => parseMarkdown(props.content))

function parseMarkdown(source: string): MarkdownBlock[] {
  const lines = source.replace(/\r\n/g, '\n').split('\n')
  const result: MarkdownBlock[] = []
  let index = 0
  let blockId = 0

  while (index < lines.length) {
    const line = lines[index]
    if (!line.trim()) {
      index += 1
      continue
    }

    const fence = line.match(/^```\s*([\w-]+)?\s*$/)
    if (fence) {
      const language = fence[1] ?? ''
      const codeLines: string[] = []
      index += 1
      while (index < lines.length && !/^```\s*$/.test(lines[index])) {
        codeLines.push(lines[index])
        index += 1
      }
      if (index < lines.length) index += 1
      result.push({ type: 'code', id: `block-${blockId++}`, language, code: codeLines.join('\n') })
      continue
    }

    const heading = line.match(/^(#{2,3})\s+(.+)$/)
    if (heading) {
      result.push({
        type: 'heading',
        id: `block-${blockId++}`,
        level: heading[1].length as 2 | 3,
        tokens: parseInline(heading[2]),
      })
      index += 1
      continue
    }

    if (/^\s*[-*]\s+/.test(line)) {
      const items: InlineToken[][] = []
      while (index < lines.length && /^\s*[-*]\s+/.test(lines[index])) {
        items.push(parseInline(lines[index].replace(/^\s*[-*]\s+/, '')))
        index += 1
      }
      result.push({ type: 'list', id: `block-${blockId++}`, items })
      continue
    }

    const paragraph: string[] = []
    while (
      index < lines.length &&
      lines[index].trim() &&
      !/^```\s*/.test(lines[index]) &&
      !/^(#{2,3})\s+/.test(lines[index]) &&
      !/^\s*[-*]\s+/.test(lines[index])
    ) {
      paragraph.push(lines[index])
      index += 1
    }
    result.push({ type: 'paragraph', id: `block-${blockId++}`, tokens: parseInline(paragraph.join(' ')) })
  }

  return result
}

function parseInline(source: string): InlineToken[] {
  const tokens: InlineToken[] = []
  const pattern = /(\*\*[^*]+\*\*|`[^`]+`)/g
  let cursor = 0
  let match: RegExpExecArray | null
  while ((match = pattern.exec(source)) !== null) {
    if (match.index > cursor) tokens.push({ type: 'text', text: source.slice(cursor, match.index) })
    const raw = match[0]
    if (raw.startsWith('`')) tokens.push({ type: 'code', text: raw.slice(1, -1) })
    else tokens.push({ type: 'strong', text: raw.slice(2, -2) })
    cursor = match.index + raw.length
  }
  if (cursor < source.length) tokens.push({ type: 'text', text: source.slice(cursor) })
  return tokens
}
</script>

<template>
  <div class="markdown-block">
    <template v-for="block in blocks" :key="block.id">
      <h2 v-if="block.type === 'heading' && block.level === 2">
        <template v-for="(token, index) in block.tokens" :key="`${block.id}-${index}`">
          <code v-if="token.type === 'code'">{{ token.text }}</code>
          <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
          <span v-else>{{ token.text }}</span>
        </template>
      </h2>
      <h3 v-else-if="block.type === 'heading'">
        <template v-for="(token, index) in block.tokens" :key="`${block.id}-${index}`">
          <code v-if="token.type === 'code'">{{ token.text }}</code>
          <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
          <span v-else>{{ token.text }}</span>
        </template>
      </h3>
      <pre v-else-if="block.type === 'code'" class="markdown-code"><code>{{ block.code }}</code></pre>
      <ul v-else-if="block.type === 'list'">
        <li v-for="(item, itemIndex) in block.items" :key="`${block.id}-${itemIndex}`">
          <template v-for="(token, tokenIndex) in item" :key="`${block.id}-${itemIndex}-${tokenIndex}`">
            <code v-if="token.type === 'code'">{{ token.text }}</code>
            <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
            <span v-else>{{ token.text }}</span>
          </template>
        </li>
      </ul>
      <p v-else>
        <template v-for="(token, index) in block.tokens" :key="`${block.id}-${index}`">
          <code v-if="token.type === 'code'">{{ token.text }}</code>
          <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
          <span v-else>{{ token.text }}</span>
        </template>
      </p>
    </template>
  </div>
</template>
