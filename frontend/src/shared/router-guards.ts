import type { Router } from 'vue-router'
import { hasAnyRole } from '@/shared/auth/permissions'
import { useAuthStore } from '@/stores/auth'

function resolveLoginRedirect(fullPath: string) {
  if (!fullPath || fullPath === '/login' || fullPath.startsWith('/login?')) {
    return undefined
  }

  if (fullPath === '/register' || fullPath.startsWith('/register?')) {
    return undefined
  }

  return fullPath
}

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to) => {
    const authStore = useAuthStore()
    await authStore.bootstrapAuth()

    if (to.meta.requiresAuth && !authStore.isAuthenticated) {
      const redirect = resolveLoginRedirect(to.fullPath)

      return redirect ? `/login?redirect=${encodeURIComponent(redirect)}` : '/login'
    }

    if (to.meta.publicOnly && authStore.isAuthenticated) {
      return '/app'
    }

    if (to.meta.requiresAuth && !hasAnyRole(authStore.userRole ?? undefined, to.meta.roles)) {
      return '/app'
    }

    if (authStore.isAuthenticated) {
      authStore.sessionExpiredReason = null
    }

    return true
  })
}
