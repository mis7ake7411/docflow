import { createRouter, createWebHistory } from 'vue-router'
import LoginPage from '@/pages/LoginPage.vue'
import DashboardPage from '@/pages/DashboardPage.vue'
import InsightsPage from '@/pages/InsightsPage.vue'
import DocumentDetailPage from '@/pages/DocumentDetailPage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', name: 'login', component: LoginPage },
    { path: '/app', name: 'dashboard', component: DashboardPage },
    { path: '/app/documents/:id', name: 'document-detail', component: DocumentDetailPage },
    { path: '/app/insights', name: 'insights', component: InsightsPage },
  ],
})

export default router
