<template>
  <section class="auth-page">
    <div class="auth-hero">
      <div class="hero-decoration"></div>
      <div class="hero-content">
        <div class="hero-badge">Invitation Only</div>
        <h1>受邀注册<br/><span class="gradient-text">完成账号创建</span></h1>
        <p class="subtitle">
          当前项目已关闭公开注册。请使用团队成员发送给你的邀请链接完成注册。
        </p>
        <ul class="auth-points">
          <li>
            <span class="point-icon">✉️</span>
            <span>邀请链接与邮箱绑定，不能随意转发给其他邮箱使用</span>
          </li>
          <li>
            <span class="point-icon">⏳</span>
            <span>邀请码过期或被使用后，将无法继续注册</span>
          </li>
          <li>
            <span class="point-icon">🎯</span>
            <span>注册成功后会直接进入岗位方向初始化</span>
          </li>
        </ul>
      </div>
    </div>

    <div class="auth-card-wrapper">
      <div class="auth-card">
        <div class="auth-heading">
          <h2>邀请注册</h2>
          <p>确认邮箱后设置昵称和密码，即可完成账号创建。</p>
        </div>

        <AppStateCard
          v-if="loading"
          variant="loading"
          title="正在校验邀请"
          message="请稍候，系统正在检查邀请是否仍然有效。"
        />

        <AppStateCard
          v-else-if="loadError"
          variant="error"
          title="邀请不可用"
          :message="loadError"
        >
          <template #actions>
            <router-link class="btn" to="/login">返回登录</router-link>
          </template>
        </AppStateCard>

        <form v-else class="auth-form" @submit.prevent="submit">
          <label class="field">
            <span>受邀邮箱</span>
            <input :value="invitation?.inviteeEmail || ''" readonly type="email" />
          </label>

          <label class="field">
            <span>昵称</span>
            <input
              v-model.trim="form.displayName"
              maxlength="50"
              placeholder="例如：philxin"
            />
          </label>

          <label class="field">
            <span>密码</span>
            <input
              v-model="form.password"
              autocomplete="new-password"
              placeholder="请输入密码"
              type="password"
            />
          </label>

          <p class="helper">密码需包含大小写字母、数字和特殊字符，长度 8-64。</p>
          <p class="helper helper-secondary">
            邀请有效期至 {{ invitation ? formatDateTime(invitation.expiresAt) : '--' }}
          </p>
          <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

          <button class="btn btn-primary submit-btn" :disabled="submitting" type="submit">
            {{ submitting ? '提交中...' : '注册并继续' }}
          </button>
        </form>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppStateCard from '../components/AppStateCard.vue'
import { authAPI } from '../api'
import { useAuthStore } from '../stores/auth'
import type { PublicRegistrationInvitation } from '../types'
import { formatDateTime } from '../utils/date'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const errorMessage = ref('')
const invitation = ref<PublicRegistrationInvitation | null>(null)
const form = reactive({
  displayName: '',
  password: '',
})

function resolveRedirect() {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
  return redirect.startsWith('/') ? redirect : '/'
}

async function loadInvitation() {
  loading.value = true
  loadError.value = ''
  try {
    const invitationCode = String(route.params.invitationCode || '')
    invitation.value = await authAPI.getInvitation(invitationCode)
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : '邀请码无效或已过期'
  } finally {
    loading.value = false
  }
}

function validate() {
  if (!form.displayName) {
    errorMessage.value = '昵称不能为空'
    return false
  }
  if (!form.password) {
    errorMessage.value = '密码不能为空'
    return false
  }
  if (!invitation.value) {
    errorMessage.value = '邀请码无效或已过期'
    return false
  }
  return true
}

async function submit() {
  if (!validate() || !invitation.value) {
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await authStore.register({
      invitationCode: invitation.value.invitationCode,
      email: invitation.value.inviteeEmail,
      password: form.password,
      displayName: form.displayName,
    })

    if (authStore.needsOnboarding) {
      await router.replace({
        path: '/onboarding',
        query: { redirect: resolveRedirect() },
      })
      return
    }
    await router.replace(resolveRedirect())
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败'
  } finally {
    submitting.value = false
  }
}

onMounted(loadInvitation)
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
  color: var(--clr-text-secondary);
}

.point-icon {
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(99, 102, 241, 0.12);
}

.auth-card-wrapper {
  display: flex;
  justify-content: center;
}

.auth-card {
  width: min(100%, 460px);
  padding: var(--sp-5);
}

.auth-heading h2 {
  margin: 0;
  font-size: var(--fs-2xl);
}

.auth-heading p {
  margin: var(--sp-2) 0 0;
  color: var(--clr-text-secondary);
}

.auth-form {
  margin-top: var(--sp-5);
  display: grid;
  gap: var(--sp-3);
}

.field {
  display: grid;
  gap: 8px;
}

.field span {
  font-size: var(--fs-sm);
  font-weight: 700;
}

.field input {
  min-height: 48px;
}

.helper {
  margin: 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-sm);
}

.helper-secondary {
  color: var(--clr-text-tertiary);
}

.error {
  margin: 0;
  color: var(--clr-danger);
  font-size: var(--fs-sm);
}

.submit-btn {
  min-height: 48px;
}

@media (max-width: 960px) {
  .auth-page {
    grid-template-columns: 1fr;
    gap: var(--sp-5);
  }
}
</style>
