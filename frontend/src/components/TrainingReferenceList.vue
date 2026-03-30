<template>
  <section class="reference-card">
    <header class="reference-head">
      <h3>{{ title }}</h3>
      <span class="count">{{ references.length }} 条</span>
    </header>

    <p v-if="references.length === 0" class="empty-text">
      {{ emptyText }}
    </p>

    <ul v-else class="reference-list">
      <li v-for="(reference, index) in references" :key="buildReferenceKey(reference, index)" class="reference-item">
        <div class="meta-row">
          <strong>{{ reference.documentTitle || '未命名文档' }}</strong>
          <span v-if="reference.similarityScore !== null && reference.similarityScore !== undefined" class="score-pill">
            相似度 {{ formatScore(reference.similarityScore) }}
          </span>
        </div>
        <p class="excerpt">{{ reference.excerpt || '暂无摘要。' }}</p>
        <p class="locator">{{ formatLocator(reference) }}</p>
      </li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import type { TrainingReference } from '../types'

withDefaults(defineProps<{
  title: string
  references: TrainingReference[]
  emptyText?: string
}>(), {
  emptyText: '当前链路未命中引用材料，系统使用了基础知识内容作为 fallback。',
})

function buildReferenceKey(reference: TrainingReference, index: number) {
  return `${reference.usageType}-${reference.chunkId ?? 'na'}-${index}`
}

function formatScore(score: number) {
  return score.toFixed(3)
}

function formatLocator(reference: TrainingReference) {
  const segments: string[] = []
  if (reference.chunkId !== null && reference.chunkId !== undefined) {
    segments.push(`chunk #${reference.chunkId}`)
  }
  if (reference.pageFrom !== null && reference.pageFrom !== undefined) {
    const pageEnd = reference.pageTo !== null && reference.pageTo !== undefined
      ? reference.pageTo
      : reference.pageFrom
    segments.push(`页码 ${reference.pageFrom}-${pageEnd}`)
  }
  if (reference.startOffset !== null && reference.startOffset !== undefined) {
    const endOffset = reference.endOffset !== null && reference.endOffset !== undefined
      ? reference.endOffset
      : reference.startOffset
    segments.push(`偏移 ${reference.startOffset}-${endOffset}`)
  }
  return segments.length > 0 ? segments.join(' · ') : '无定位信息'
}
</script>

<style scoped>
.reference-card {
  display: grid;
  gap: var(--sp-3);
  padding: var(--sp-4);
  border: 1px solid var(--clr-border);
  border-radius: var(--radius-md);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.9), rgba(255, 255, 255, 1));
}

.reference-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-2);
}

.reference-head h3 {
  margin: 0;
  font-size: var(--fs-base);
  font-weight: 700;
}

.count {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 2px 10px;
  border-radius: var(--radius-full);
  background: var(--clr-primary-50);
  color: var(--clr-primary-dark);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.empty-text {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--clr-text-secondary);
}

.reference-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--sp-2);
}

.reference-item {
  display: grid;
  gap: var(--sp-1);
  padding: var(--sp-3);
  border-radius: var(--radius-sm);
  background: var(--clr-surface);
  border: 1px solid var(--clr-border);
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
  align-items: center;
}

.meta-row strong {
  font-size: var(--fs-sm);
  font-weight: 700;
}

.score-pill {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: var(--radius-full);
  background: rgba(6, 182, 212, 0.12);
  color: var(--clr-accent-dark);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.excerpt {
  margin: 0;
  font-size: var(--fs-sm);
  color: var(--clr-text-secondary);
  line-height: 1.6;
}

.locator {
  margin: 0;
  font-size: var(--fs-xs);
  color: var(--clr-text-tertiary);
}
</style>
