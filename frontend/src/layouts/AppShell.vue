<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowDown,
  Close,
  Expand,
  Promotion,
  SwitchButton,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { appNavigationItems, isNavigationActive } from './appNavigation'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const showNav = computed(() => !route.matched.some((record) => record.meta.guestOnly))
const mobileMenuOpen = ref(false)

const activeMenu = computed(() => {
  const matchedItem = appNavigationItems.find((item) => isNavigationActive(item, route.path))
  return matchedItem?.to ?? ''
})

const userInitial = computed(() => authStore.user?.displayName?.charAt(0)?.toUpperCase() || '?')
const userRoleText = computed(() => authStore.user?.targetRole || '待选择方向')

watch(() => route.fullPath, closeMobileMenu)

function openMobileMenu() {
  mobileMenuOpen.value = true
}

function closeMobileMenu() {
  mobileMenuOpen.value = false
}

function navigateTo(path: string) {
  closeMobileMenu()
  if (route.path !== path) {
    void router.push(path)
  }
}

async function logout() {
  authStore.logout()
  closeMobileMenu()
  await router.replace('/login')
}

async function handleUserCommand(command: string | number | object) {
  if (command === 'logout') {
    await logout()
  }
}
</script>

<template>
  <el-config-provider>
    <el-container class="app-shell">
      <el-header v-if="showNav" class="shell-header">
        <div class="shell-header-inner">
          <router-link to="/" class="brand" @click="closeMobileMenu">
            <span class="brand-mark">
              <el-icon><Promotion /></el-icon>
            </span>
            <span class="brand-copy">
              <strong>InterviewOS</strong>
              <small>面试训练工作台</small>
            </span>
          </router-link>

          <el-menu
            v-if="authStore.isAuthenticated"
            :default-active="activeMenu"
            class="shell-menu desktop-only"
            mode="horizontal"
            router
          >
            <el-menu-item v-for="item in appNavigationItems" :key="item.key" :index="item.to">
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </el-menu-item>
          </el-menu>

          <div v-if="authStore.isAuthenticated && authStore.user" class="shell-actions">
            <el-button class="mobile-only" circle text type="primary" @click="openMobileMenu">
              <el-icon><Expand /></el-icon>
            </el-button>

            <el-dropdown trigger="click" @command="handleUserCommand">
              <el-button class="user-trigger" text>
                <el-avatar :size="36" class="user-avatar">{{ userInitial }}</el-avatar>
                <span class="user-meta desktop-only-inline">
                  <strong>{{ authStore.user.displayName }}</strong>
                  <small>{{ userRoleText }}</small>
                </span>
                <el-icon class="desktop-only-inline"><ArrowDown /></el-icon>
              </el-button>

              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">
                    <el-icon><SwitchButton /></el-icon>
                    <span>退出登录</span>
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>

      <el-main class="shell-main" :class="{ 'shell-main-guest': !showNav }">
        <router-view />
      </el-main>

      <el-drawer
        v-model="mobileMenuOpen"
        :with-header="false"
        class="mobile-drawer"
        direction="rtl"
        size="300px"
      >
        <div class="drawer-head">
          <div>
            <strong>导航</strong>
            <p>快速切换核心训练页面</p>
          </div>
          <el-button circle text @click="closeMobileMenu">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>

        <div class="drawer-nav">
          <el-button
            v-for="item in appNavigationItems"
            :key="item.key"
            :class="['drawer-link', { 'drawer-link-active': isNavigationActive(item, route.path) }]"
            text
            @click="navigateTo(item.to)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </el-button>
        </div>

        <el-divider />

        <el-button class="drawer-logout" type="danger" plain @click="logout">
          <el-icon><SwitchButton /></el-icon>
          <span>退出登录</span>
        </el-button>
      </el-drawer>
    </el-container>
  </el-config-provider>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(ellipse at 20% 0%, rgba(99, 102, 241, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 80% 100%, rgba(6, 182, 212, 0.05) 0%, transparent 50%),
    var(--clr-bg);
}

.shell-header {
  position: sticky;
  top: 0;
  z-index: 20;
  height: auto;
  padding: 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.7);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(16px) saturate(180%);
  -webkit-backdrop-filter: blur(16px) saturate(180%);
}

.shell-header-inner {
  max-width: calc(var(--max-width) + 48px);
  min-height: var(--nav-height);
  margin: 0 auto;
  padding: 0 var(--sp-5);
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--sp-4);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.brand-mark {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: inline-grid;
  place-items: center;
  color: var(--clr-text-inverse);
  background: linear-gradient(135deg, var(--clr-primary), var(--clr-accent));
  box-shadow: var(--shadow-glow);
  font-size: 18px;
}

.brand-copy {
  display: grid;
  gap: 2px;
}

.brand-copy strong {
  font-size: var(--fs-lg);
  font-weight: 800;
  color: var(--clr-text);
  letter-spacing: -0.02em;
}

.brand-copy small {
  color: var(--clr-text-tertiary);
  font-size: var(--fs-xs);
}

.shell-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--sp-2);
}

.user-trigger {
  padding: 6px 8px;
  border-radius: 14px;
}

.user-avatar {
  background: linear-gradient(135deg, var(--clr-primary), var(--clr-accent));
  color: var(--clr-text-inverse);
  font-weight: 700;
}

.user-meta {
  margin: 0 6px 0 10px;
  display: grid;
  text-align: left;
  gap: 1px;
}

.user-meta strong {
  font-size: var(--fs-sm);
  color: var(--clr-text);
}

.user-meta small {
  font-size: var(--fs-xs);
  color: var(--clr-text-tertiary);
}

.shell-main {
  max-width: var(--max-width);
  width: 100%;
  margin: 0 auto;
  padding: var(--sp-6) var(--sp-5) var(--sp-10);
}

.shell-main-guest {
  max-width: 1200px;
}

.drawer-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--sp-3);
}

.drawer-head p {
  margin: 4px 0 0;
  color: var(--clr-text-secondary);
  font-size: var(--fs-sm);
}

.drawer-nav {
  display: grid;
  gap: var(--sp-2);
}

.drawer-link {
  justify-content: flex-start;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  color: var(--clr-text-secondary);
}

.drawer-link-active {
  color: var(--clr-primary);
  background: var(--clr-primary-50);
}

.drawer-logout {
  width: 100%;
}

.mobile-only,
.mobile-drawer {
  display: none;
}

:deep(.shell-menu.el-menu--horizontal) {
  border-bottom: none;
  background: transparent;
  min-width: 0;
}

:deep(.shell-menu .el-menu-item) {
  height: 44px;
  margin: 0;
  border-radius: 12px;
  color: var(--clr-text-secondary);
  border-bottom: none;
  gap: 6px;
  font-weight: 600;
}

:deep(.shell-menu .el-menu-item:hover),
:deep(.shell-menu .el-menu-item.is-active) {
  color: var(--clr-primary);
  background: var(--clr-primary-50);
}

:deep(.shell-menu .el-menu-item.is-active) {
  border-bottom: none;
}

:deep(.mobile-drawer .el-drawer__body) {
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  gap: var(--sp-4);
  padding-top: var(--sp-2);
}

@media (max-width: 960px) {
  .shell-header-inner {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .desktop-only,
  .desktop-only-inline {
    display: none;
  }

  .mobile-only {
    display: inline-flex;
  }

  .mobile-drawer {
    display: block;
  }
}

@media (max-width: 640px) {
  .shell-main {
    padding: var(--sp-4) var(--sp-4) var(--sp-8);
  }

  .shell-header-inner {
    padding: 0 var(--sp-4);
  }

  .brand-copy small {
    display: none;
  }
}
</style>
