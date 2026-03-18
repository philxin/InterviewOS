<template>
  <div class="inline-state" :class="`state-${variant}`">
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
  margin-top: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px 14px;
  background: #f8fafc;
  color: #475569;
  line-height: 1.7;
}

.state-loading {
  border-color: #bfdbfe;
  background: #eff6ff;
  color: #1d4ed8;
}

.state-error {
  border-color: #fecaca;
  background: #fff1f2;
  color: #b91c1c;
}
</style>
