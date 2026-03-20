<template>
  <section class="auth-page">
    <div class="auth-hero">
      <div class="hero-decoration"></div>
      <div class="hero-content">
        <div class="hero-badge">InterviewOS V2</div>
        <h1>面试提升<br/><span class="gradient-text">从这里开始</span></h1>
        <p class="subtitle">
          登录后进入你的训练数据和会话主链路，全面追踪你的面试准备进度。
        </p>
        <ul class="auth-points">
          <li>
            <span class="point-icon">🔒</span>
            <span>知识点、训练历史和结果页全部按用户隔离</span>
          </li>
          <li>
            <span class="point-icon">🔄</span>
            <span>登录成功后自动恢复到原本想访问的页面</span>
          </li>
          <li>
            <span class="point-icon">🎯</span>
            <span>首次注册后会进入岗位方向初始化</span>
          </li>
        </ul>
      </div>
    </div>

    <div class="auth-card-wrapper">
      <div class="auth-card">
        <div class="mode-switch">
          <button
            class="mode-btn"
            :class="{ active: mode === 'login' }"
            type="button"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            class="mode-btn"
            :class="{ active: mode === 'register' }"
            type="button"
            @click="switchMode('register')"
          >
            注册
          </button>
          <div class="mode-slider" :class="{ right: mode === 'register' }"></div>
        </div>

        <form class="auth-form" @submit.prevent="submit">
          <label v-if="mode === 'register'" class="field animate-slideUp">
            <span>昵称</span>
            <input
              v-model.trim="registerForm.displayName"
              maxlength="50"
              placeholder="例如：philxin"
            />
          </label>

          <label class="field">
            <span>邮箱</span>
            <input
              v-model.trim="currentForm.email"
              autocomplete="email"
              placeholder="name@example.com"
              type="email"
            />
          </label>

          <label class="field">
            <span>密码</span>
            <input
              v-model="currentForm.password"
              :autocomplete="mode === 'login' ? 'current-password' : 'new-password'"
              placeholder="请输入密码"
              type="password"
            />
          </label>

          <p v-if="mode === 'register'" class="helper">
            密码需包含大小写字母、数字和特殊字符，长度 8-64。
          </p>

          <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

          <button class="btn btn-primary submit-btn" :disabled="submitting" type="submit">
            {{ submitting ? '提交中...' : mode === 'login' ? '登录并继续' : '注册并继续' }}
          </button>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const mode = ref<'login' | 'register'>('login')
const submitting = ref(false)
const errorMessage = ref('')

const loginForm = reactive({
  email: '',
  password: '',
})

const registerForm = reactive({
  displayName: '',
  email: '',
  password: '',
})

const currentForm = computed(() => (mode.value === 'login' ? loginForm : registerForm))

function switchMode(nextMode: 'login' | 'register') {
  mode.value = nextMode
  errorMessage.value = ''
}

function validate() {
  if (!currentForm.value.email) {
    errorMessage.value = '邮箱不能为空'
    return false
  }
  if (!currentForm.value.password) {
    errorMessage.value = '密码不能为空'
    return false
  }
  if (mode.value === 'register' && !registerForm.displayName) {
    errorMessage.value = '昵称不能为空'
    return false
  }
  return true
}

function resolveRedirect() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  return redirect.startsWith('/') ? redirect : '/'
}

async function submit() {
  if (!validate()) {
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    if (mode.value === 'login') {
      await authStore.login(loginForm)
    } else {
      await authStore.register(registerForm)
    }

    if (authStore.needsOnboarding) {
      await router.replace({
        path: '/onboarding',
        query: { redirect: resolveRedirect() },
      })
      return
    }
    await router.replace(resolveRedirect())
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '认证失败'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: calc(100vh - var(--nav-height));
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: var(--sp-8);
  align-items: center;
  padding: var(--sp-6) 0;
}

/* Hero Section */
.auth-hero {
  position: relative;
  padding: var(--sp-6) var(--sp-5);
  overflow: hidden;
}

.hero-decoration {
  position: absolute;
  top: -80px;
  left: -60px;
  width: 400px;
  height: 400px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(99, 102, 241, 0.12), transparent 70%);
  pointer-events: none;
}

.hero-content {
  position: relative;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  padding: 6px 14px;
  border-radius: var(--radius-full);
  background: linear-gradient(135deg, var(--clr-primary-50), rgba(6, 182, 212, 0.08));
  border: 1px solid rgba(99, 102, 241, 0.2);
  color: var(--clr-primary);
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  margin-bottom: var(--sp-4);
}

.auth-hero h1 {
  margin: 0;
  font-size: clamp(2.2rem, 5vw, 3.5rem);
  line-height: 1.1;
  font-weight: 800;
  color: var(--clr-text);
  letter-spacing: -0.03em;
}

.gradient-text {
  background: linear-gradient(135deg, var(--clr-primary), var(--clr-accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  margin: var(--sp-5) 0 0;
  max-width: 50ch;
  color: var(--clr-text-secondary);
  font-size: var(--fs-base);
  line-height: 1.7;
}

.auth-points {
  margin: var(--sp-6) 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--sp-3);
}

.auth-points li {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
  font-size: var(--fs-sm);
  color: var(--clr-text-secondary);
}

.point-icon {
  font-size: 1.2rem;
  flex-shrink: 0;
}

/* Auth Card */
.auth-card-wrapper {
  display: flex;
  justify-content: center;
}

.auth-card {
  width: 100%;
  max-width: 420px;
  padding: var(--sp-6);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  animation: slideUp 0.5s var(--ease-out);
}

/* Mode Switch */
.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  position: relative;
  padding: 4px;
  gap: 4px;
  background: var(--clr-bg-secondary);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.mode-btn {
  position: relative;
  z-index: 1;
  border: 0;
  background: transparent;
  border-radius: var(--radius-sm);
  padding: 12px;
  font-weight: 700;
  font-size: var(--fs-sm);
  color: var(--clr-text-tertiary);
  cursor: pointer;
  transition: color var(--duration-normal) var(--ease-out);
}

.mode-btn.active {
  color: var(--clr-text);
}

.mode-slider {
  position: absolute;
  top: 4px;
  left: 4px;
  width: calc(50% - 4px);
  height: calc(100% - 8px);
  background: var(--clr-surface);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-md);
  transition: transform var(--duration-normal) var(--ease-out);
}

.mode-slider.right {
  transform: translateX(calc(100% + 4px));
}

/* Form */
.auth-form {
  display: grid;
  gap: var(--sp-4);
  margin-top: var(--sp-5);
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-size: var(--fs-sm);
  font-weight: 600;
  color: var(--clr-text-secondary);
}

.field input {
  width: 100%;
  border: 1.5px solid var(--clr-border);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  background: var(--clr-surface);
  font-size: var(--fs-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.field input:focus {
  outline: none;
  border-color: var(--clr-primary);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.helper {
  margin: -4px 0 0;
  color: var(--clr-text-tertiary);
  font-size: var(--fs-xs);
}

.error {
  margin: 0;
  color: var(--clr-danger);
  font-size: var(--fs-sm);
  font-weight: 500;
}

.submit-btn {
  width: 100%;
  padding: 13px 20px;
  font-size: var(--fs-base);
  border-radius: var(--radius-md);
  margin-top: var(--sp-1);
}

/* Responsive */
@media (max-width: 920px) {
  .auth-page {
    grid-template-columns: 1fr;
    min-height: auto;
    padding: var(--sp-4);
    gap: var(--sp-4);
  }

  .auth-hero {
    padding: var(--sp-2) 0 0;
  }

  .hero-decoration {
    width: 250px;
    height: 250px;
    top: -40px;
    left: -30px;
  }

  .auth-card-wrapper {
    justify-content: stretch;
  }

  .auth-card {
    max-width: 100%;
  }
}

@media (max-width: 480px) {
  .auth-hero h1 {
    font-size: 1.8rem;
  }

  .auth-card {
    padding: var(--sp-4);
  }
}
</style>
