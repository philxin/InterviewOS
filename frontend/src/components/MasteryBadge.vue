<template>
  <span class="mastery-badge" :class="levelClass">{{ levelText }} · {{ mastery }}%</span>
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
  gap: 4px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.expert {
  color: #166534;
  background: #dcfce7;
}

.proficient {
  color: #155e75;
  background: #cffafe;
}

.competent {
  color: #92400e;
  background: #fef3c7;
}

.beginner {
  color: #1e3a8a;
  background: #dbeafe;
}
</style>
