import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to) => {
    const authStore = useAuthStore()
    await authStore.bootstrapAuth()

    if (to.path !== '/login' && !authStore.isAuthenticated) {
      return '/login'
    }

    if (to.path === '/login' && authStore.isAuthenticated) {
      return '/app'
    }

    return true
  })
}
