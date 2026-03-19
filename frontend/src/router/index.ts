import { createRouter, createWebHistory } from 'vue-router'
import { appRoutes } from '@/router/routes'

const router = createRouter({
  history: createWebHistory(),
  routes: appRoutes,
})

export default router
