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
    case 'STRONG':
      return 'strong'
    case 'GOOD':
      return 'good'
    case 'BASIC':
      return 'basic'
    case 'INCOMPLETE':
      return 'incomplete'
    default:
      return 'unclear'
  }
})

const fallbackDescription = computed(() => {
  switch (props.band?.code) {
    case 'STRONG':
      return '内容完整、表达自然、深度较好。'
    case 'GOOD':
      return '结构较清晰，关键点基本覆盖。'
    case 'BASIC':
      return '方向正确，但深度不足。'
    case 'INCOMPLETE':
      return '方向基本正确，但缺少关键细节。'
    default:
      return '答案结构混乱、难以判断理解程度。'
  }
})
</script>

<style scoped>
.band-card {
  display: grid;
  gap: 6px;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid transparent;
}

.band-card strong {
  font-size: 24px;
  line-height: 1.1;
}

.band-card p {
  margin: 0;
  color: #334155;
}

.eyebrow {
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  font-weight: 700;
}

.strong {
  background: #ecfdf5;
  border-color: #86efac;
  color: #166534;
}

.good {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.basic {
  background: #fffbeb;
  border-color: #fcd34d;
  color: #b45309;
}

.incomplete {
  background: #fff7ed;
  border-color: #fdba74;
  color: #c2410c;
}

.unclear {
  background: #fef2f2;
  border-color: #fca5a5;
  color: #b91c1c;
}
</style>
