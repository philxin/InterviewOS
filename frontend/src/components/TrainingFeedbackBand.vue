<template>
  <div v-if="band" class="band-card" :class="toneClass">
    <span class="eyebrow">当前档位</span>
    <strong>{{ band.label }}</strong>
    <p>{{ band.description || fallbackDescription }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FeedbackBand } from '../types'

const props = defineProps<{
  band: FeedbackBand | null
}>()

const toneClass = computed(() => {
  switch (props.band?.code) {
    case 'STRONG': return 'strong'
    case 'GOOD': return 'good'
    case 'BASIC': return 'basic'
    case 'INCOMPLETE': return 'incomplete'
    default: return 'unclear'
  }
})

const fallbackDescription = computed(() => {
  switch (props.band?.code) {
    case 'STRONG': return '内容完整、表达自然、深度较好。'
    case 'GOOD': return '结构较清晰，关键点基本覆盖。'
    case 'BASIC': return '方向正确，但深度不足。'
    case 'INCOMPLETE': return '方向基本正确，但缺少关键细节。'
    default: return '答案结构混乱、难以判断理解程度。'
  }
})
</script>

<style scoped>
.band-card {
  display: grid;
  gap: var(--sp-2);
  padding: var(--sp-5);
  border-radius: var(--radius-xl);
  border: 1px solid transparent;
  animation: slideUp var(--duration-slow) var(--ease-out);
  position: relative;
  overflow: hidden;
}

.band-card::before {
  content: '';
  position: absolute;
  top: -50%;
  right: -50%;
  width: 150px;
  height: 150px;
  border-radius: 50%;
  background: radial-gradient(circle, currentColor, transparent 70%);
  opacity: 0.06;
  pointer-events: none;
}

.band-card strong {
  font-size: var(--fs-2xl);
  line-height: 1.1;
  font-weight: 800;
}

.band-card p {
  margin: 0;
  font-size: var(--fs-sm);
  opacity: 0.85;
  line-height: 1.6;
}

.eyebrow {
  font-size: var(--fs-xs);
  letter-spacing: 0.12em;
  text-transform: uppercase;
  font-weight: 700;
  opacity: 0.7;
}

.strong {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  border-color: var(--clr-success-border);
  color: #166534;
}

.good {
  background: linear-gradient(135deg, #eef2ff, #e0e7ff);
  border-color: #93c5fd;
  color: #1d4ed8;
}

.basic {
  background: linear-gradient(135deg, #fffbeb, #fef3c7);
  border-color: var(--clr-warning-border);
  color: #b45309;
}

.incomplete {
  background: linear-gradient(135deg, #fff7ed, #ffedd5);
  border-color: #fdba74;
  color: #c2410c;
}

.unclear {
  background: linear-gradient(135deg, #fef2f2, #fee2e2);
  border-color: var(--clr-danger-border);
  color: var(--clr-danger);
}
</style>
