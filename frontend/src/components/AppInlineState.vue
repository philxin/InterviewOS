<template>
  <div class="inline-state" :class="`state-${variant}`">
    <span v-if="variant === 'loading'" class="inline-spinner"></span>
    {{ text }}
  </div>
</template>

<script setup lang="ts">
type InlineStateVariant = 'info' | 'loading' | 'empty' | 'error'

withDefaults(defineProps<{
  variant?: InlineStateVariant
  text: string
}>(), {
  variant: 'info',
})
</script>

<style scoped>
.inline-state {
  margin-top: var(--sp-4);
  border: 1px solid var(--clr-border);
  border-radius: var(--radius-md);
  padding: var(--sp-3) var(--sp-4);
  background: var(--clr-bg-secondary);
  color: var(--clr-text-secondary);
  line-height: 1.7;
  font-size: var(--fs-sm);
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  animation: fadeIn var(--duration-normal) var(--ease-out);
}

.inline-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid var(--clr-border);
  border-top-color: var(--clr-primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  flex-shrink: 0;
}

.state-loading {
  border-color: rgba(99, 102, 241, 0.15);
  background: var(--clr-primary-50);
  color: var(--clr-primary-dark);
}

.state-error {
  border-color: var(--clr-danger-border);
  background: var(--clr-danger-bg);
  color: var(--clr-danger);
}
</style>
