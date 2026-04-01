import { describe, it, expect, beforeEach, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
// @ts-ignore
import { useAuthStore } from '@/stores/auth'

describe('Auth Store - Session Expiry', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  it('應初始化時 sessionExpiredReason 為 null', () => {
    const authStore = useAuthStore()
    expect(authStore.sessionExpiredReason).toBeNull()
  })

  it('呼叫 setSessionExpired 應設置過期原因並清空 token', () => {
    const authStore = useAuthStore()
    authStore.setAuth({
      accessToken: 'test-token',
      refreshToken: 'test-refresh',
      user: {
        id: 1,
        username: 'test',
        email: 'test@example.com',
        role: 'USER',
        status: 'ACTIVE',
        mustChangePassword: false,
      },
    })

    const reason = '您的登入已過期'
    authStore.setSessionExpired(reason)

    expect(authStore.sessionExpiredReason).toBe(reason)
    expect(authStore.accessToken).toBeNull()
    expect(authStore.refreshToken).toBeNull()
    expect(authStore.user).toBeNull()
  })

  it('setSessionExpired 應清空本地存儲', () => {
    const authStore = useAuthStore()
    authStore.setAuth({
      accessToken: 'test-token',
      refreshToken: 'test-refresh',
      user: {
        id: 1,
        username: 'test',
        email: 'test@example.com',
        role: 'USER',
        status: 'ACTIVE',
        mustChangePassword: false,
      },
    })

    authStore.setSessionExpired('過期')

    expect(localStorage.getItem('docflow.accessToken')).toBeNull()
    expect(localStorage.getItem('docflow.refreshToken')).toBeNull()
    expect(localStorage.getItem('docflow.user')).toBeNull()
  })

  it('clearAuth 應清空過期原因', () => {
    const authStore = useAuthStore()
    authStore.sessionExpiredReason = '過期'
    authStore.clearAuth()

    expect(authStore.sessionExpiredReason).toBeNull()
  })

  it('setSessionExpired 不傳參數應使用預設訊息', () => {
    const authStore = useAuthStore()
    authStore.setSessionExpired()

    expect(authStore.sessionExpiredReason).toBe('您的登入已過期，請重新登入')
  })
})

