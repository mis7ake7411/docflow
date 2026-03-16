import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function setupRouterGuards(router: Router) {
  router.beforeEach((to) => {
    const authStore = useAuthStore()
    const isAuthenticated = authStore.isAuthenticated

    if (to.path !== '/login' && !isAuthenticated) {
      return '/login'
    }

    if (to.path === '/login' && isAuthenticated) {
      return '/app'
    }

    return true
  })
}
