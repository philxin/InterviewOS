<template>
  <div class="card state-card" :class="`state-${variant}`">
    <p v-if="title" class="state-title">{{ title }}</p>
    <p class="state-message">{{ message }}</p>
    <div v-if="$slots.actions" class="state-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script setup lang="ts">
type StateVariant = 'info' | 'loading' | 'empty' | 'error'

withDefaults(defineProps<{
  variant?: StateVariant
  title?: string
  message: string
}>(), {
  variant: 'info',
  title: '',
})
</script>

<style scoped>
.state-card {
  padding: 20px;
  display: grid;
  gap: 8px;
}

.state-title,
.state-message {
  margin: 0;
}

.state-title {
  font-size: 18px;
  font-weight: 700;
}

.state-message {
  color: #475569;
  line-height: 1.7;
}

.state-actions {
  margin-top: 6px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.state-loading {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.state-loading .state-message {
  color: #1d4ed8;
}

.state-empty {
  background: #f8fafc;
  border-color: #e2e8f0;
}

.state-error {
  background: #fff1f2;
  border-color: #fecaca;
}

.state-error .state-title,
.state-error .state-message {
  color: #b91c1c;
}
</style>
