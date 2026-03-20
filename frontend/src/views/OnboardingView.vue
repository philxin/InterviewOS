<template>
  <section class="onboarding-page">
    <header class="onboarding-header animate-fadeIn">
      <div class="header-badge">训练方向初始化</div>
      <h1>选择你的<span class="gradient-text">主攻方向</span></h1>
      <p>
        后续题目和反馈会围绕你选的方向收敛，帮你做到精准训练。
      </p>
    </header>

    <div class="role-grid">
      <button
        v-for="(role, index) in roles"
        :key="role.value"
        class="role-card"
        :class="{ active: selectedRole === role.value }"
        :style="{ animationDelay: `${index * 80}ms` }"
        type="button"
        @click="selectedRole = role.value"
      >
        <div class="role-icon">{{ roleIcons[role.value] }}</div>
        <div class="role-content">
          <strong>{{ role.label }}</strong>
          <span>{{ role.description }}</span>
        </div>
        <div class="role-check">
          <svg v-if="selectedRole === role.value" width="20" height="20" viewBox="0 0 20 20" fill="none">
            <circle cx="10" cy="10" r="10" fill="currentColor"/>
            <path d="M6 10l3 3 5-5" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <div v-else class="check-empty"></div>
        </div>
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

const roleIcons: Record<string, string> = {
  JAVA_BACKEND: '☕',
  FRONTEND: '🎨',
  FULLSTACK: '🔗',
  DEVOPS: '🚀',
  DATA_ENGINEER: '📊',
}

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
  gap: var(--sp-6);
  max-width: 780px;
  margin: 0 auto;
}

.onboarding-header {
  text-align: center;
}

.header-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.1), rgba(6, 182, 212, 0.1));
  border: 1px solid rgba(16, 185, 129, 0.2);
  color: var(--clr-success);
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  margin-bottom: var(--sp-4);
}

.onboarding-header h1 {
  margin: 0;
  font-size: clamp(1.8rem, 4vw, 2.5rem);
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.gradient-text {
  background: linear-gradient(135deg, var(--clr-primary), var(--clr-accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.onboarding-header p {
  margin: var(--sp-3) auto 0;
  color: var(--clr-text-secondary);
  max-width: 48ch;
  line-height: 1.6;
}

.role-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--sp-3);
}

.role-card {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  padding: var(--sp-4) var(--sp-5);
  text-align: left;
  border-radius: var(--radius-lg);
  background: var(--clr-surface);
  border: 1.5px solid var(--clr-border);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
  animation: slideUp 0.4s var(--ease-out) both;
}

.role-card:hover {
  border-color: var(--clr-primary-light);
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.role-card.active {
  border-color: var(--clr-primary);
  background: linear-gradient(135deg, var(--clr-primary-50), rgba(6, 182, 212, 0.04));
  box-shadow: 0 8px 30px rgba(99, 102, 241, 0.12), var(--shadow-glow);
}

.role-icon {
  font-size: 1.8rem;
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-md);
  background: var(--clr-bg-secondary);
}

.role-card.active .role-icon {
  background: rgba(99, 102, 241, 0.1);
}

.role-content {
  flex: 1;
  min-width: 0;
}

.role-content strong {
  display: block;
  font-size: var(--fs-base);
  font-weight: 700;
  margin-bottom: 2px;
}

.role-content span {
  font-size: var(--fs-sm);
  color: var(--clr-text-secondary);
  line-height: 1.4;
}

.role-check {
  flex-shrink: 0;
  color: var(--clr-primary);
}

.check-empty {
  width: 20px;
  height: 20px;
  border: 2px solid var(--clr-border);
  border-radius: 50%;
}

.role-card.active .check-empty {
  border-color: var(--clr-primary);
}

.error {
  margin: 0;
  color: var(--clr-danger);
  font-weight: 500;
  text-align: center;
}

.footer-actions {
  display: flex;
  justify-content: space-between;
  gap: var(--sp-3);
}

@media (max-width: 768px) {
  .role-grid {
    grid-template-columns: 1fr;
  }

  .footer-actions {
    flex-direction: column;
  }

  .footer-actions .btn {
    width: 100%;
  }
}
</style>
