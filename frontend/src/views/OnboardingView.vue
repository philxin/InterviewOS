<template>
  <section class="onboarding-page">
    <header class="onboarding-header">
      <p class="eyebrow">训练方向初始化</p>
      <h1>先选一个主要岗位方向，后续题目和反馈会围绕它收敛。</h1>
      <p>
        这是 V2 的最小 onboarding。当前只保存一项 `targetRole`，后面再逐步扩到训练偏好。
      </p>
    </header>

    <div class="role-grid">
      <button
        v-for="role in roles"
        :key="role.value"
        class="card role-card"
        :class="{ active: selectedRole === role.value }"
        type="button"
        @click="selectedRole = role.value"
      >
        <strong>{{ role.label }}</strong>
        <span>{{ role.description }}</span>
      </button>
    </div>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <footer class="footer-actions">
      <button class="btn" type="button" @click="logout">退出登录</button>
      <button class="btn btn-primary" :disabled="saving || !selectedRole" type="button" @click="submit">
        {{ saving ? '保存中...' : '保存并进入系统' }}
      </button>
    </footer>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import type { TargetRole } from '../types'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const saving = ref(false)
const errorMessage = ref('')
const selectedRole = ref<TargetRole | null>(authStore.user?.targetRole ?? null)

const roles: Array<{ value: TargetRole; label: string; description: string }> = [
  { value: 'JAVA_BACKEND', label: 'Java 后端', description: 'Spring、JVM、数据库、分布式链路。' },
  { value: 'FRONTEND', label: '前端工程', description: 'Vue/React、浏览器机制、工程化和性能。' },
  { value: 'FULLSTACK', label: '全栈开发', description: '前后端协同、接口设计和交付能力。' },
  { value: 'DEVOPS', label: 'DevOps', description: 'CI/CD、容器、可观测性和发布稳定性。' },
  { value: 'DATA_ENGINEER', label: '数据工程', description: 'ETL、数据建模、计算链路和稳定性。' },
]

async function submit() {
  if (!selectedRole.value) {
    errorMessage.value = '请选择一个岗位方向'
    return
  }

  saving.value = true
  errorMessage.value = ''
  try {
    await authStore.updateOnboarding(selectedRole.value)
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    await router.replace(redirect.startsWith('/') ? redirect : '/')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存训练方向失败'
  } finally {
    saving.value = false
  }
}

async function logout() {
  authStore.logout()
  await router.replace('/login')
}
</script>

<style scoped>
.onboarding-page {
  display: grid;
  gap: 18px;
}

.onboarding-header {
  max-width: 68ch;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #0f766e;
  font-weight: 700;
}

.onboarding-header h1 {
  margin: 0;
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.1;
}

.onboarding-header p:last-child {
  margin: 12px 0 0;
  color: #475569;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.role-card {
  padding: 18px;
  text-align: left;
  border-radius: 18px;
  background: linear-gradient(180deg, #fff 0%, #f8fafc 100%);
  border: 1px solid #dbe4f0;
  cursor: pointer;
  display: grid;
  gap: 8px;
}

.role-card strong {
  font-size: 18px;
}

.role-card span {
  color: #475569;
}

.role-card.active {
  border-color: #0f766e;
  background: linear-gradient(180deg, #f0fdfa 0%, #ecfeff 100%);
  box-shadow: 0 18px 40px rgba(15, 118, 110, 0.12);
}

.error {
  margin: 0;
  color: #b91c1c;
}

.footer-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

@media (max-width: 768px) {
  .role-grid {
    grid-template-columns: 1fr;
  }

  .footer-actions {
    flex-direction: column;
  }
}
</style>
