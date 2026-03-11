import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('../views/DashboardView.vue')
  },
  {
    path: '/knowledge/new',
    name: 'KnowledgeNew',
    component: () => import('../views/KnowledgeFormView.vue')
  },
  {
    path: '/knowledge/edit/:id',
    name: 'KnowledgeEdit',
    component: () => import('../views/KnowledgeFormView.vue')
  },
  {
    path: '/training/:knowledgeId',
    name: 'Training',
    component: () => import('../views/TrainingView.vue')
  },
  {
    path: '/result',
    name: 'Result',
    component: () => import('../views/ResultView.vue')
  },
  {
    path: '/history',
    name: 'History',
    component: () => import('../views/HistoryView.vue')
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
