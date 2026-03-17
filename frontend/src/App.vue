<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const showNav = computed(() => route.path !== '/login')

async function logout() {
  authStore.logout()
  await router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <nav v-if="showNav" class="top-nav">
      <div class="top-nav-inner">
        <div class="brand">InterviewOS</div>
        <div class="nav-links">
          <router-link v-if="authStore.isAuthenticated" to="/">知识点</router-link>
          <router-link v-if="authStore.isAuthenticated" to="/knowledge/new">新建</router-link>
          <router-link v-if="authStore.isAuthenticated" to="/knowledge/import">导入</router-link>
          <router-link v-if="authStore.isAuthenticated" to="/history">历史</router-link>
        </div>
        <div v-if="authStore.isAuthenticated && authStore.user" class="session-actions">
          <div class="user-chip">
            <strong>{{ authStore.user.displayName }}</strong>
            <span>{{ authStore.user.targetRole || '待选择方向' }}</span>
          </div>
          <button class="btn" type="button" @click="logout">退出</button>
        </div>
      </div>
    </nav>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: linear-gradient(180deg, #f8fafc 0%, #eef2ff 100%);
}

.top-nav {
  border-bottom: 1px solid #e2e8f0;
  background: #ffffffcc;
  backdrop-filter: blur(8px);
}

.top-nav-inner {
  max-width: 1120px;
  margin: 0 auto;
  height: 64px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.nav-links {
  display: flex;
  gap: 16px;
}

.session-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-chip {
  display: grid;
  gap: 2px;
  text-align: right;
}

.user-chip strong {
  font-size: 14px;
  color: #0f172a;
}

.user-chip span {
  font-size: 12px;
  color: #64748b;
}

.nav-links a {
  color: #334155;
  font-weight: 600;
}

.nav-links a.router-link-active {
  color: #1d4ed8;
}

.content {
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px 20px 40px;
}

@media (max-width: 768px) {
  .top-nav-inner {
    height: auto;
    padding-top: 14px;
    padding-bottom: 14px;
    align-items: flex-start;
    flex-direction: column;
  }

  .session-actions {
    width: 100%;
    justify-content: space-between;
  }
}
</style>
