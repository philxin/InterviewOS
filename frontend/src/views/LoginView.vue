<template>
  <section class="auth-page">
    <div class="auth-shell">
      <div class="auth-hero">
        <div class="hero-grid">
          <div class="hero-copy">
            <div class="hero-badge">InterviewOS Access</div>
            <h1>登录后继续你的<br><span class="gradient-text">面试训练主链路</span></h1>
            <p class="subtitle">
              进入知识点看板、推荐训练和结果复盘页，沿着同一条训练路径持续推进准备节奏。
            </p>
          </div>

          <div class="hero-highlights">
            <article class="highlight-card">
              <span>训练看板</span>
              <strong>当天推荐与回练提醒集中处理</strong>
            </article>
            <article class="highlight-card">
              <span>结果追踪</span>
              <strong>最近训练、分数变化和薄弱项一屏查看</strong>
            </article>
            <article class="highlight-card">
              <span>账号机制</span>
              <strong>登录账号与邀请注册链路分开，访问边界更清晰</strong>
            </article>
          </div>

          <div class="hero-panel">
            <span class="panel-kicker">登录前说明</span>
            <ul class="auth-points">
              <li>
                <span class="point-icon">01</span>
                <div class="point-copy">
                  <strong>已有账号可直接登录</strong>
                  <span>使用已完成邀请注册的邮箱和密码进入系统。</span>
                </div>
              </li>
              <li>
                <span class="point-icon">02</span>
                <div class="point-copy">
                  <strong>没有账号先拿邀请链接</strong>
                  <span>当前不开放公开注册，需要由已注册用户发起邀请。</span>
                </div>
              </li>
              <li>
                <span class="point-icon">03</span>
                <div class="point-copy">
                  <strong>登录后自动回到原页面</strong>
                  <span>如果你是从某个功能页跳转过来，认证成功后会自动恢复。</span>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>

      <div class="auth-card-wrapper">
        <div class="auth-card">
          <div class="auth-heading">
            <span class="auth-eyebrow">已有账号</span>
            <h2>登录并继续</h2>
            <p>输入你的邮箱和密码，回到当前训练进度。</p>
          </div>

          <form class="auth-form" @submit.prevent="submit">
            <label class="field">
              <span>邮箱</span>
              <input
                v-model.trim="loginForm.email"
                autocomplete="email"
                placeholder="name@example.com"
                type="email"
              />
            </label>

            <label class="field">
              <span>密码</span>
              <input
                v-model="loginForm.password"
                autocomplete="current-password"
                placeholder="请输入密码"
                type="password"
              />
            </label>

            <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
            <p class="helper">如果你还没有账号，请联系团队内已注册用户发起邀请。</p>

            <button class="btn btn-primary submit-btn" :disabled="submitting" type="submit">
              {{ submitting ? '提交中...' : '登录并继续' }}
            </button>
          </form>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const submitting = ref(false)
const errorMessage = ref('')

const loginForm = reactive({
  email: '',
  password: '',
})

function validate() {
  if (!loginForm.email) {
    errorMessage.value = '邮箱不能为空'
    return false
  }
  if (!loginForm.password) {
    errorMessage.value = '密码不能为空'
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
    await authStore.login(loginForm)

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
  padding: var(--sp-6) 0;
}

.auth-shell {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(360px, 0.9fr);
  gap: var(--sp-6);
  align-items: stretch;
}

.auth-hero {
  position: relative;
  padding: var(--sp-6);
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: var(--radius-2xl);
  background:
    radial-gradient(circle at top left, rgba(99, 102, 241, 0.15), transparent 34%),
    radial-gradient(circle at bottom right, rgba(6, 182, 212, 0.14), transparent 36%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.96), rgba(241, 245, 249, 0.92));
  box-shadow: var(--shadow-md);
  overflow: hidden;
}

.auth-hero::before {
  position: absolute;
  content: '';
  inset: auto -80px -120px auto;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(99, 102, 241, 0.08);
  filter: blur(10px);
  pointer-events: none;
}

.auth-hero::after {
  position: absolute;
  content: '';
  inset: -120px auto auto -60px;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  background: rgba(6, 182, 212, 0.08);
  filter: blur(10px);
  pointer-events: none;
}

.hero-grid {
  position: relative;
  z-index: 1;
  display: grid;
  gap: var(--sp-5);
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
}

.hero-copy {
  display: grid;
  gap: var(--sp-4);
}

.hero-copy h1 {
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
  max-width: 50ch;
  margin: 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-base);
  line-height: 1.7;
}

.hero-highlights {
  display: grid;
  gap: var(--sp-3);
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.highlight-card {
  display: grid;
  gap: var(--sp-2);
  padding: var(--sp-4);
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(226, 232, 240, 0.78);
  box-shadow: var(--shadow-sm);
}

.highlight-card span {
  color: var(--clr-primary-dark);
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.highlight-card strong {
  font-size: var(--fs-sm);
  line-height: 1.6;
}

.hero-panel {
  display: grid;
  gap: var(--sp-4);
  padding: var(--sp-5);
  border-radius: var(--radius-xl);
  background: linear-gradient(160deg, rgba(15, 23, 42, 0.95), rgba(30, 41, 59, 0.92));
  color: var(--clr-text-inverse);
  box-shadow: var(--shadow-lg);
}

.panel-kicker {
  color: rgba(255, 255, 255, 0.68);
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-points {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: var(--sp-3);
}

.auth-points li {
  display: grid;
  grid-template-columns: auto 1fr;
  align-items: start;
  gap: var(--sp-3);
  padding: var(--sp-3);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.point-icon {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  color: rgba(255, 255, 255, 0.86);
  font-size: var(--fs-xs);
  font-weight: 800;
}

.point-copy {
  display: grid;
  gap: 2px;
}

.point-copy strong {
  font-size: var(--fs-sm);
}

.point-copy span {
  color: rgba(255, 255, 255, 0.72);
  font-size: var(--fs-xs);
  line-height: 1.6;
}

.auth-card-wrapper {
  display: flex;
  align-items: stretch;
}

.auth-card {
  width: 100%;
  padding: var(--sp-6);
  border-radius: var(--radius-2xl);
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(226, 232, 240, 0.6);
  box-shadow:
    0 20px 60px rgba(0, 0, 0, 0.06),
    0 0 0 1px rgba(255, 255, 255, 0.6) inset;
  animation: slideUp 0.5s var(--ease-out);
}

.auth-heading {
  display: grid;
  gap: var(--sp-2);
}

.auth-eyebrow {
  color: var(--clr-primary-dark);
  font-size: var(--fs-xs);
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.auth-heading h2 {
  margin: 0;
  font-size: var(--fs-3xl);
  line-height: 1.1;
  letter-spacing: -0.03em;
}

.auth-heading p {
  margin: 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-sm);
  line-height: 1.6;
}

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

@media (max-width: 920px) {
  .auth-shell {
    grid-template-columns: 1fr;
    gap: var(--sp-4);
  }

  .hero-highlights {
    grid-template-columns: 1fr;
  }

  .auth-page {
    min-height: auto;
    padding: var(--sp-4) 0;
  }

  .auth-hero,
  .auth-card {
    padding: var(--sp-5);
  }
}

@media (max-width: 640px) {
  .hero-copy h1 {
    font-size: 2rem;
  }

  .auth-heading h2 {
    font-size: var(--fs-2xl);
  }

  .auth-points li {
    grid-template-columns: 1fr;
  }

  .point-icon {
    width: 30px;
    height: 30px;
  }
}

@media (max-width: 480px) {
  .auth-card {
    padding: var(--sp-4);
  }

  .auth-hero {
    padding: var(--sp-4);
  }
}
</style>
