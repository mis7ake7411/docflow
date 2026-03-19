import type { Router } from 'vue-router'
import { hasAnyRole } from '@/shared/auth/permissions'
import { useAuthStore } from '@/stores/auth'

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to) => {
    const authStore = useAuthStore()
    await authStore.bootstrapAuth()

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      return '/login'
    }

    if (to.meta.publicOnly && authStore.isAuthenticated) {
      return '/app'
    }

    if (to.meta.requiresAuth && !hasAnyRole(authStore.userRole ?? undefined, to.meta.roles)) {
      return '/app'
    }

    return true
  })
}
