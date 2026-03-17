<template>
  <section class="auth-page">
    <div class="auth-hero">
      <p class="eyebrow">InterviewOS V2</p>
      <h1>登录后再进入你的训练数据和会话主链路。</h1>
      <p class="subtitle">
        前端已切到真实 Bearer Token 鉴权。这里直接对接 `/auth/login` 和 `/auth/register`，
        不再依赖本地假登录。
      </p>
      <ul class="auth-points">
        <li>知识点、训练历史和结果页全部按用户隔离</li>
        <li>登录成功后自动恢复到原本想访问的页面</li>
        <li>首次注册后会进入岗位方向初始化</li>
      </ul>
    </div>

    <div class="card auth-card">
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
      </div>

      <form class="auth-form" @submit.prevent="submit">
        <label v-if="mode === 'register'" class="field">
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
  min-height: calc(100vh - 48px);
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 28px;
  align-items: center;
}

.auth-hero {
  padding: 24px 8px;
}

.eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: #9a3412;
  font-weight: 700;
}

.auth-hero h1 {
  margin: 0;
  font-size: clamp(34px, 5vw, 54px);
  line-height: 1.04;
  color: #111827;
}

.subtitle {
  margin: 18px 0 0;
  max-width: 56ch;
  color: #475569;
  font-size: 16px;
}

.auth-points {
  margin: 24px 0 0;
  padding-left: 18px;
  color: #334155;
  display: grid;
  gap: 8px;
}

.auth-card {
  padding: 18px;
  border-radius: 24px;
  border-color: #f1d2b5;
  background:
    radial-gradient(circle at top, rgba(251, 191, 36, 0.14), transparent 40%),
    #fffdf8;
  box-shadow: 0 24px 60px rgba(148, 63, 15, 0.08);
}

.mode-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 4px;
  gap: 6px;
  background: #f3e8d8;
  border-radius: 16px;
}

.mode-btn {
  border: 0;
  background: transparent;
  border-radius: 12px;
  padding: 12px;
  font-weight: 700;
  color: #7c2d12;
}

.mode-btn.active {
  background: #fff;
  color: #111827;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
}

.auth-form {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.field {
  display: grid;
  gap: 6px;
}

.field span {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.field input {
  width: 100%;
  border: 1px solid #d6d3d1;
  border-radius: 14px;
  padding: 12px 14px;
  background: #fff;
}

.helper {
  margin: -2px 0 0;
  color: #78716c;
  font-size: 13px;
}

.error {
  margin: 0;
  color: #b91c1c;
}

.submit-btn {
  width: 100%;
  padding: 12px 18px;
}

@media (max-width: 920px) {
  .auth-page {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .auth-hero {
    padding: 8px 0 0;
  }
}
</style>
