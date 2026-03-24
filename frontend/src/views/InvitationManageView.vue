<template>
  <section class="invitation-page">
    <header class="page-header">
      <div>
        <h1>邀请注册</h1>
        <p>只有已注册用户生成的邀请链接，才能让新用户完成注册。</p>
      </div>
    </header>

    <div class="invite-grid">
      <section class="card invite-form-card">
        <h2>创建邀请</h2>
        <p class="section-copy">邀请链接与邮箱绑定，且只能使用一次。</p>

        <form class="invite-form" @submit.prevent="submit">
          <label class="field">
            <span>被邀请邮箱</span>
            <input
              v-model.trim="form.email"
              autocomplete="email"
              placeholder="candidate@example.com"
              type="email"
            />
          </label>

          <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
          <p v-if="successMessage" class="success">{{ successMessage }}</p>

          <button class="btn btn-primary" type="submit" :disabled="submitting">
            {{ submitting ? '创建中...' : '生成邀请链接' }}
          </button>
        </form>
      </section>

      <section class="card invite-result-card">
        <template v-if="invitation">
          <h2>邀请结果</h2>
          <dl class="result-list">
            <div>
              <dt>邀请邮箱</dt>
              <dd>{{ invitation.inviteeEmail }}</dd>
            </div>
            <div>
              <dt>过期时间</dt>
              <dd>{{ formatDateTime(invitation.expiresAt) }}</dd>
            </div>
            <div>
              <dt>注册链接</dt>
              <dd class="link-block">{{ invitationLink }}</dd>
            </div>
          </dl>

          <div class="result-actions">
            <button class="btn btn-primary" type="button" @click="copyInvitationLink">
              复制链接
            </button>
          </div>
        </template>
        <AppStateCard
          v-else
          variant="empty"
          title="尚未生成邀请"
          message="输入邮箱并创建邀请后，注册链接会显示在这里。"
        />
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import AppStateCard from '../components/AppStateCard.vue'
import { invitationAPI } from '../api'
import type { RegistrationInvitation } from '../types'
import { formatDateTime } from '../utils/date'

const form = reactive({
  email: '',
})

const submitting = ref(false)
const errorMessage = ref('')
const successMessage = ref('')
const invitation = ref<RegistrationInvitation | null>(null)

const invitationLink = computed(() => {
  if (!invitation.value) {
    return ''
  }
  if (typeof window === 'undefined') {
    return invitation.value.registrationPath
  }
  return `${window.location.origin}${invitation.value.registrationPath}`
})

async function submit() {
  if (!form.email) {
    errorMessage.value = '邮箱不能为空'
    return
  }

  submitting.value = true
  errorMessage.value = ''
  successMessage.value = ''

  try {
    invitation.value = await invitationAPI.create({ email: form.email })
    successMessage.value = '邀请已生成，请将链接发送给对方完成注册。'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '创建邀请失败'
  } finally {
    submitting.value = false
  }
}

async function copyInvitationLink() {
  if (!invitationLink.value) {
    return
  }
  try {
    await navigator.clipboard.writeText(invitationLink.value)
    successMessage.value = '邀请链接已复制到剪贴板。'
    errorMessage.value = ''
  } catch {
    errorMessage.value = '复制失败，请手动复制链接。'
  }
}
</script>

<style scoped>
.invitation-page {
  display: grid;
  gap: var(--sp-5);
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-4);
}

.page-header h1 {
  margin: 0;
  font-size: var(--fs-3xl);
  font-weight: 800;
  letter-spacing: -0.02em;
}

.page-header p {
  margin: var(--sp-1) 0 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-sm);
}

.invite-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.95fr) minmax(0, 1.05fr);
  gap: var(--sp-4);
}

.card {
  padding: var(--sp-5);
}

.card h2 {
  margin: 0;
  font-size: var(--fs-xl);
}

.section-copy {
  margin: var(--sp-2) 0 0;
  color: var(--clr-text-secondary);
}

.invite-form {
  margin-top: var(--sp-4);
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
  color: var(--clr-text);
}

.field input {
  min-height: 48px;
}

.error,
.success {
  margin: 0;
  font-size: var(--fs-sm);
}

.error {
  color: var(--clr-danger);
}

.success {
  color: #15803d;
}

.result-list {
  margin: var(--sp-4) 0 0;
  display: grid;
  gap: var(--sp-3);
}

.result-list div {
  display: grid;
  gap: 6px;
}

.result-list dt {
  font-size: var(--fs-xs);
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--clr-text-tertiary);
}

.result-list dd {
  margin: 0;
  color: var(--clr-text);
  word-break: break-word;
}

.link-block {
  padding: var(--sp-3);
  border-radius: var(--radius-md);
  background: var(--clr-bg-secondary);
  border: 1px dashed var(--clr-border);
}

.result-actions {
  margin-top: var(--sp-4);
  display: flex;
  gap: var(--sp-2);
}

@media (max-width: 900px) {
  .invite-grid {
    grid-template-columns: 1fr;
  }
}
</style>
