<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const showNav = computed(() => route.path !== '/login')
const mobileMenuOpen = ref(false)

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

async function logout() {
  authStore.logout()
  mobileMenuOpen.value = false
  await router.replace('/login')
}
</script>

<template>
  <div class="app-shell">
    <nav v-if="showNav" class="top-nav">
      <div class="top-nav-inner">
        <div class="nav-left">
          <router-link to="/" class="brand" @click="closeMobileMenu">
            <span class="brand-icon">⚡</span>
            <span class="brand-text">InterviewOS</span>
          </router-link>
          <button
            v-if="authStore.isAuthenticated"
            class="hamburger"
            type="button"
            :class="{ open: mobileMenuOpen }"
            @click="toggleMobileMenu"
            aria-label="菜单"
          >
            <span></span>
            <span></span>
            <span></span>
          </button>
        </div>

        <div
          v-if="authStore.isAuthenticated"
          class="nav-links"
          :class="{ show: mobileMenuOpen }"
        >
          <router-link to="/" @click="closeMobileMenu">
            <span class="nav-icon">📋</span> 知识点
          </router-link>
          <router-link to="/knowledge/new" @click="closeMobileMenu">
            <span class="nav-icon">➕</span> 新建
          </router-link>
          <router-link to="/knowledge/import" @click="closeMobileMenu">
            <span class="nav-icon">📥</span> 导入
          </router-link>
          <router-link to="/knowledge/file-import" @click="closeMobileMenu">
            <span class="nav-icon">📄</span> 文件导入
          </router-link>
          <router-link to="/history" @click="closeMobileMenu">
            <span class="nav-icon">📊</span> 历史
          </router-link>
        </div>

        <div v-if="authStore.isAuthenticated && authStore.user" class="session-actions">
          <div class="user-chip">
            <div class="user-avatar">{{ authStore.user.displayName?.charAt(0) || '?' }}</div>
            <div class="user-info">
              <strong>{{ authStore.user.displayName }}</strong>
              <span>{{ authStore.user.targetRole || '待选择方向' }}</span>
            </div>
          </div>
          <button class="btn btn-logout" type="button" @click="logout">退出</button>
        </div>
      </div>
    </nav>
    <main class="content animate-fadeIn">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(99, 102, 241, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 100%, rgba(6, 182, 212, 0.05) 0%, transparent 50%),
    var(--clr-bg);
}

/* ===== Navigation ===== */
.top-nav {
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid rgba(226, 232, 240, 0.6);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
}

.top-nav-inner {
  max-width: var(--max-width);
  margin: 0 auto;
  height: var(--nav-height);
  padding: 0 var(--sp-5);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-4);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: var(--sp-4);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
  text-decoration: none;
}

.brand-icon {
  font-size: 1.5rem;
}

.brand-text {
  font-size: var(--fs-xl);
  font-weight: 800;
  background: linear-gradient(135deg, var(--clr-primary), var(--clr-accent));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  letter-spacing: -0.02em;
}

/* Nav Links */
.nav-links {
  display: flex;
  align-items: center;
  gap: var(--sp-1);
}

.nav-links a {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--clr-text-secondary);
  font-weight: 600;
  font-size: var(--fs-sm);
  padding: 8px 14px;
  border-radius: var(--radius-sm);
  transition: all var(--duration-fast) var(--ease-out);
}

.nav-links a:hover {
  color: var(--clr-primary);
  background: var(--clr-primary-50);
}

.nav-links a.router-link-active,
.nav-links a.router-link-exact-active {
  color: var(--clr-primary);
  background: var(--clr-primary-50);
  font-weight: 700;
}

.nav-icon {
  font-size: 1rem;
  line-height: 1;
}

/* Session */
.session-actions {
  display: flex;
  align-items: center;
  gap: var(--sp-3);
}

.user-chip {
  display: flex;
  align-items: center;
  gap: var(--sp-2);
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--clr-primary), var(--clr-accent));
  color: white;
  display: grid;
  place-items: center;
  font-weight: 700;
  font-size: var(--fs-sm);
  flex-shrink: 0;
}

.user-info {
  display: grid;
  gap: 1px;
  text-align: right;
}

.user-info strong {
  font-size: var(--fs-sm);
  color: var(--clr-text);
  line-height: 1.3;
}

.user-info span {
  font-size: var(--fs-xs);
  color: var(--clr-text-tertiary);
  line-height: 1.3;
}

.btn-logout {
  padding: 7px 14px;
  font-size: var(--fs-xs);
  border-radius: var(--radius-sm);
}

/* Hamburger */
.hamburger {
  display: none;
  flex-direction: column;
  gap: 5px;
  padding: 6px;
  background: none;
  border: none;
  cursor: pointer;
}

.hamburger span {
  display: block;
  width: 22px;
  height: 2px;
  background: var(--clr-text);
  border-radius: 2px;
  transition: all var(--duration-normal) var(--ease-out);
  transform-origin: center;
}

.hamburger.open span:nth-child(1) {
  transform: rotate(45deg) translate(5px, 5px);
}
.hamburger.open span:nth-child(2) {
  opacity: 0;
}
.hamburger.open span:nth-child(3) {
  transform: rotate(-45deg) translate(5px, -5px);
}

/* Content */
.content {
  max-width: var(--max-width);
  margin: 0 auto;
  padding: var(--sp-6) var(--sp-5) var(--sp-10);
}

/* ===== Mobile ===== */
@media (max-width: 900px) {
  .hamburger {
    display: flex;
  }

  .nav-links {
    display: none;
    position: absolute;
    top: var(--nav-height);
    left: 0;
    right: 0;
    flex-direction: column;
    background: rgba(255, 255, 255, 0.95);
    backdrop-filter: blur(16px);
    -webkit-backdrop-filter: blur(16px);
    border-bottom: 1px solid var(--clr-border);
    padding: var(--sp-3);
    box-shadow: var(--shadow-lg);
    animation: slideDown 0.25s var(--ease-out);
  }

  .nav-links.show {
    display: flex;
  }

  .nav-links a {
    width: 100%;
    padding: 12px 16px;
    border-radius: var(--radius-sm);
    font-size: var(--fs-base);
  }

  .user-info {
    display: none;
  }

  .session-actions {
    gap: var(--sp-2);
  }
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 480px) {
  .content {
    padding: var(--sp-4) var(--sp-4) var(--sp-8);
  }
}
</style>
