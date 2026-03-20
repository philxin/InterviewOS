<template>
  <span class="mastery-badge" :class="levelClass">
    <span class="mastery-dot"></span>
    {{ levelText }} · {{ mastery }}%
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  mastery: number
}>()

const levelText = computed(() => {
  if (props.mastery >= 80) return '精通'
  if (props.mastery >= 60) return '熟练'
  if (props.mastery >= 40) return '掌握'
  return '入门'
})

const levelClass = computed(() => {
  if (props.mastery >= 80) return 'expert'
  if (props.mastery >= 60) return 'proficient'
  if (props.mastery >= 40) return 'competent'
  return 'beginner'
})
</script>

<style scoped>
.mastery-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  border-radius: var(--radius-full);
  font-size: var(--fs-xs);
  font-weight: 700;
  white-space: nowrap;
  transition: all var(--duration-fast) var(--ease-out);
}

.mastery-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: currentColor;
  flex-shrink: 0;
}

.expert {
  color: #166534;
  background: linear-gradient(135deg, #dcfce7, #bbf7d0);
  border: 1px solid #86efac;
}

.proficient {
  color: #0e7490;
  background: linear-gradient(135deg, #cffafe, #a5f3fc);
  border: 1px solid #67e8f9;
}

.competent {
  color: #92400e;
  background: linear-gradient(135deg, #fef3c7, #fde68a);
  border: 1px solid #fcd34d;
}

.beginner {
  color: #1e40af;
  background: linear-gradient(135deg, #dbeafe, #bfdbfe);
  border: 1px solid #93c5fd;
}
</style>
