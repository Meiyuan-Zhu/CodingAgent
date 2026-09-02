<script setup lang="ts">
import { computed } from 'vue'

type InlineToken =
  | { type: 'text'; text: string }
  | { type: 'code'; text: string }
  | { type: 'strong'; text: string }
  | { type: 'emphasis'; text: string }
  | { type: 'link'; text: string; href: string }

type MarkdownBlock =
  | { type: 'paragraph'; id: string; tokens: InlineToken[] }
  | { type: 'heading'; id: string; level: 1 | 2 | 3; tokens: InlineToken[] }
  | { type: 'code'; id: string; language: string; code: string }
  | { type: 'list'; id: string; ordered: boolean; items: InlineToken[][] }
  | { type: 'table'; id: string; headers: InlineToken[][]; rows: InlineToken[][][] }
  | { type: 'quote'; id: string; tokens: InlineToken[] }
  | { type: 'rule'; id: string }

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

    const heading = line.match(/^(#{1,3})\s+(.+)$/)
    if (heading) {
      result.push({
        type: 'heading',
        id: `block-${blockId++}`,
        level: heading[1].length as 1 | 2 | 3,
        tokens: parseInline(heading[2]),
      })
      index += 1
      continue
    }

    if (/^\s*>\s?/.test(line)) {
      const quoteLines: string[] = []
      while (index < lines.length && /^\s*>\s?/.test(lines[index])) {
        quoteLines.push(lines[index].replace(/^\s*>\s?/, ''))
        index += 1
      }
      result.push({ type: 'quote', id: `block-${blockId++}`, tokens: parseInline(quoteLines.join(' ')) })
      continue
    }

    if (/^\s*(-{3,}|\*{3,})\s*$/.test(line)) {
      result.push({ type: 'rule', id: `block-${blockId++}` })
      index += 1
      continue
    }

    if (isTableStart(lines, index)) {
      const headers = splitTableRow(lines[index]).map(parseInline)
      const rows: InlineToken[][][] = []
      index += 2
      while (index < lines.length && isPipeRow(lines[index])) {
        rows.push(splitTableRow(lines[index]).map(parseInline))
        index += 1
      }
      result.push({ type: 'table', id: `block-${blockId++}`, headers, rows })
      continue
    }

    if (/^\s*(?:[-*]|\d+\.)\s+/.test(line)) {
      const ordered = /^\s*\d+\.\s+/.test(line)
      const items: InlineToken[][] = []
      const itemPattern = ordered ? /^\s*\d+\.\s+/ : /^\s*[-*]\s+/
      while (index < lines.length && itemPattern.test(lines[index])) {
        items.push(parseInline(lines[index].replace(itemPattern, '')))
        index += 1
      }
      result.push({ type: 'list', id: `block-${blockId++}`, ordered, items })
      continue
    }

    const paragraph: string[] = []
    while (
      index < lines.length &&
      lines[index].trim() &&
      !/^```\s*/.test(lines[index]) &&
      !/^(#{1,3})\s+/.test(lines[index]) &&
      !/^\s*>\s?/.test(lines[index]) &&
      !/^\s*(-{3,}|\*{3,})\s*$/.test(lines[index]) &&
      !isTableStart(lines, index) &&
      !/^\s*(?:[-*]|\d+\.)\s+/.test(lines[index])
    ) {
      paragraph.push(lines[index])
      index += 1
    }
    result.push({ type: 'paragraph', id: `block-${blockId++}`, tokens: parseInline(paragraph.join(' ')) })
  }

  return result
}

function isTableStart(lines: string[], index: number) {
  if (index + 1 >= lines.length) return false
  return isPipeRow(lines[index]) && /^\s*\|?\s*:?-{3,}:?\s*(?:\|\s*:?-{3,}:?\s*)+\|?\s*$/.test(lines[index + 1])
}

function isPipeRow(line: string) {
  const trimmed = line.trim()
  return trimmed.includes('|') && !trimmed.startsWith('```')
}

function splitTableRow(line: string) {
  let trimmed = line.trim()
  if (trimmed.startsWith('|')) trimmed = trimmed.slice(1)
  if (trimmed.endsWith('|')) trimmed = trimmed.slice(0, -1)
  return trimmed.split('|').map((cell) => cell.trim())
}

function parseInline(source: string): InlineToken[] {
  const tokens: InlineToken[] = []
  const pattern = /(\[[^\]]+\]\([^)]+\)|\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`)/g
  let cursor = 0
  let match: RegExpExecArray | null
  while ((match = pattern.exec(source)) !== null) {
    if (match.index > cursor) tokens.push({ type: 'text', text: source.slice(cursor, match.index) })
    const raw = match[0]
    if (raw.startsWith('`')) {
      tokens.push({ type: 'code', text: raw.slice(1, -1) })
    } else if (raw.startsWith('**')) {
      tokens.push({ type: 'strong', text: raw.slice(2, -2) })
    } else if (raw.startsWith('*')) {
      tokens.push({ type: 'emphasis', text: raw.slice(1, -1) })
    } else {
      const link = raw.match(/^\[([^\]]+)\]\(([^)]+)\)$/)
      if (link) {
        tokens.push({ type: 'link', text: link[1], href: safeHref(link[2]) })
      }
    }
    cursor = match.index + raw.length
  }
  if (cursor < source.length) tokens.push({ type: 'text', text: source.slice(cursor) })
  return tokens
}

function safeHref(value: string) {
  const trimmed = value.trim()
  if (/^(https?:|mailto:)/i.test(trimmed)) return trimmed
  if (trimmed.startsWith('#') || trimmed.startsWith('/')) return trimmed
  return '#'
}
</script>

<template>
  <div class="markdown-block">
    <template v-for="block in blocks" :key="block.id">
      <h1 v-if="block.type === 'heading' && block.level === 1">
        <template v-for="(token, index) in block.tokens" :key="`${block.id}-${index}`">
          <code v-if="token.type === 'code'">{{ token.text }}</code>
          <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
          <em v-else-if="token.type === 'emphasis'">{{ token.text }}</em>
          <a v-else-if="token.type === 'link'" :href="token.href" target="_blank" rel="noreferrer">{{ token.text }}</a>
          <span v-else>{{ token.text }}</span>
        </template>
      </h1>
      <h2 v-else-if="block.type === 'heading' && block.level === 2">
        <template v-for="(token, index) in block.tokens" :key="`${block.id}-${index}`">
          <code v-if="token.type === 'code'">{{ token.text }}</code>
          <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
          <em v-else-if="token.type === 'emphasis'">{{ token.text }}</em>
          <a v-else-if="token.type === 'link'" :href="token.href" target="_blank" rel="noreferrer">{{ token.text }}</a>
          <span v-else>{{ token.text }}</span>
        </template>
      </h2>
      <h3 v-else-if="block.type === 'heading'">
        <template v-for="(token, index) in block.tokens" :key="`${block.id}-${index}`">
          <code v-if="token.type === 'code'">{{ token.text }}</code>
          <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
          <em v-else-if="token.type === 'emphasis'">{{ token.text }}</em>
          <a v-else-if="token.type === 'link'" :href="token.href" target="_blank" rel="noreferrer">{{ token.text }}</a>
          <span v-else>{{ token.text }}</span>
        </template>
      </h3>
      <pre v-else-if="block.type === 'code'" class="markdown-code"><code>{{ block.code }}</code></pre>
      <blockquote v-else-if="block.type === 'quote'">
        <template v-for="(token, index) in block.tokens" :key="`${block.id}-${index}`">
          <code v-if="token.type === 'code'">{{ token.text }}</code>
          <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
          <em v-else-if="token.type === 'emphasis'">{{ token.text }}</em>
          <a v-else-if="token.type === 'link'" :href="token.href" target="_blank" rel="noreferrer">{{ token.text }}</a>
          <span v-else>{{ token.text }}</span>
        </template>
      </blockquote>
      <hr v-else-if="block.type === 'rule'">
      <ol v-else-if="block.type === 'list' && block.ordered">
        <li v-for="(item, itemIndex) in block.items" :key="`${block.id}-${itemIndex}`">
          <template v-for="(token, tokenIndex) in item" :key="`${block.id}-${itemIndex}-${tokenIndex}`">
            <code v-if="token.type === 'code'">{{ token.text }}</code>
            <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
            <em v-else-if="token.type === 'emphasis'">{{ token.text }}</em>
            <a v-else-if="token.type === 'link'" :href="token.href" target="_blank" rel="noreferrer">{{ token.text }}</a>
            <span v-else>{{ token.text }}</span>
          </template>
        </li>
      </ol>
      <div v-else-if="block.type === 'table'" class="markdown-table-wrap">
        <table>
          <thead>
            <tr>
              <th v-for="(header, headerIndex) in block.headers" :key="`${block.id}-header-${headerIndex}`">
                <template v-for="(token, tokenIndex) in header" :key="`${block.id}-header-${headerIndex}-${tokenIndex}`">
                  <code v-if="token.type === 'code'">{{ token.text }}</code>
                  <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
                  <em v-else-if="token.type === 'emphasis'">{{ token.text }}</em>
                  <a v-else-if="token.type === 'link'" :href="token.href" target="_blank" rel="noreferrer">{{ token.text }}</a>
                  <span v-else>{{ token.text }}</span>
                </template>
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, rowIndex) in block.rows" :key="`${block.id}-row-${rowIndex}`">
              <td v-for="(cell, cellIndex) in row" :key="`${block.id}-row-${rowIndex}-${cellIndex}`">
                <template v-for="(token, tokenIndex) in cell" :key="`${block.id}-row-${rowIndex}-${cellIndex}-${tokenIndex}`">
                  <code v-if="token.type === 'code'">{{ token.text }}</code>
                  <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
                  <em v-else-if="token.type === 'emphasis'">{{ token.text }}</em>
                  <a v-else-if="token.type === 'link'" :href="token.href" target="_blank" rel="noreferrer">{{ token.text }}</a>
                  <span v-else>{{ token.text }}</span>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <ul v-else-if="block.type === 'list'">
        <li v-for="(item, itemIndex) in block.items" :key="`${block.id}-${itemIndex}`">
          <template v-for="(token, tokenIndex) in item" :key="`${block.id}-${itemIndex}-${tokenIndex}`">
            <code v-if="token.type === 'code'">{{ token.text }}</code>
            <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
            <em v-else-if="token.type === 'emphasis'">{{ token.text }}</em>
            <a v-else-if="token.type === 'link'" :href="token.href" target="_blank" rel="noreferrer">{{ token.text }}</a>
            <span v-else>{{ token.text }}</span>
          </template>
        </li>
      </ul>
      <p v-else>
        <template v-for="(token, index) in block.tokens" :key="`${block.id}-${index}`">
          <code v-if="token.type === 'code'">{{ token.text }}</code>
          <strong v-else-if="token.type === 'strong'">{{ token.text }}</strong>
          <em v-else-if="token.type === 'emphasis'">{{ token.text }}</em>
          <a v-else-if="token.type === 'link'" :href="token.href" target="_blank" rel="noreferrer">{{ token.text }}</a>
          <span v-else>{{ token.text }}</span>
        </template>
      </p>
    </template>
  </div>
</template>
