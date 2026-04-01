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

    // 登入成功後清空 session 過期原因
    if (to.name === 'login' && authStore.sessionExpiredReason) {
      // 保留原因顯示一次
      authStore.sessionExpiredReason = authStore.sessionExpiredReason
    } else if (authStore.isAuthenticated) {
      // 認証通過後清空過期原因
      authStore.sessionExpiredReason = null
    }

    return true
  })
}
