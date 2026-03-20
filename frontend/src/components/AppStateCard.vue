<template>
  <div class="card state-card" :class="`state-${variant}`">
    <div v-if="variant === 'loading'" class="state-spinner"></div>
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
  padding: var(--sp-6);
  display: grid;
  gap: var(--sp-2);
  justify-items: center;
  text-align: center;
  animation: fadeIn var(--duration-slow) var(--ease-out);
}

.state-title,
.state-message {
  margin: 0;
}

.state-title {
  font-size: var(--fs-lg);
  font-weight: 700;
}

.state-message {
  color: var(--clr-text-secondary);
  line-height: 1.7;
  max-width: 48ch;
}

.state-actions {
  margin-top: var(--sp-2);
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2);
}

.state-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--clr-border);
  border-top-color: var(--clr-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: var(--sp-1);
}

.state-loading {
  background: linear-gradient(135deg, var(--clr-primary-50), rgba(6, 182, 212, 0.04));
  border-color: rgba(99, 102, 241, 0.15);
}

.state-loading .state-message {
  color: var(--clr-primary-dark);
}

.state-empty {
  background: var(--clr-bg-secondary);
  border-color: var(--clr-border);
}

.state-error {
  background: var(--clr-danger-bg);
  border-color: var(--clr-danger-border);
}

.state-error .state-title,
.state-error .state-message {
  color: var(--clr-danger);
}
</style>
