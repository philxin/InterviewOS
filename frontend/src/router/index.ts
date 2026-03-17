import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { pinia } from '../stores'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/LoginView.vue'),
    meta: { guestOnly: true },
  },
  {
    path: '/onboarding',
    name: 'Onboarding',
    component: () => import('../views/OnboardingView.vue'),
    meta: { requiresAuth: true, allowWithoutOnboarding: true },
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/DashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/knowledge/new',
    name: 'KnowledgeNew',
    component: () => import('../views/KnowledgeFormView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/knowledge/import',
    name: 'KnowledgeImport',
    component: () => import('../views/KnowledgeImportView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/knowledge/edit/:id',
    name: 'KnowledgeEdit',
    component: () => import('../views/KnowledgeFormView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/training/:knowledgeId',
    name: 'Training',
    component: () => import('../views/TrainingView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/training/session/:sessionId',
    name: 'TrainingSession',
    component: () => import('../views/TrainingSessionView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/result/:sessionId',
    name: 'Result',
    component: () => import('../views/ResultView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('../views/HistoryView.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore(pinia)
  await authStore.bootstrap()

  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const guestOnly = to.matched.some((record) => record.meta.guestOnly)
  const allowWithoutOnboarding = to.matched.some((record) => record.meta.allowWithoutOnboarding)

  if (requiresAuth && !authStore.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath },
    }
  }

  if (guestOnly && authStore.isAuthenticated) {
    return authStore.needsOnboarding ? '/onboarding' : '/'
  }

  if (authStore.isAuthenticated && authStore.needsOnboarding && !allowWithoutOnboarding) {
    return {
      path: '/onboarding',
      query: { redirect: to.fullPath },
    }
  }

  if (to.path === '/onboarding' && authStore.isAuthenticated && !authStore.needsOnboarding) {
    return '/'
  }

  return true
})

export default router
