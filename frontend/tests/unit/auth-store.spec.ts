import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import {
  login as loginApi,
  logout as logoutApi,
  register as registerApi,
} from '@/features/auth/api'
import { useAuthStore } from '@/stores/auth'

vi.mock('@/features/auth/api', () => ({
  changePassword: vi.fn(),
  getCurrentUser: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  refreshToken: vi.fn(),
  register: vi.fn(),
}))

class MemoryStorage implements Storage {
  private readonly items = new Map<string, string>()

  get length() {
    return this.items.size
  }

  clear() {
    this.items.clear()
  }

  getItem(key: string) {
    return this.items.has(key) ? this.items.get(key) ?? null : null
  }

  key(index: number) {
    return Array.from(this.items.keys())[index] ?? null
  }

  removeItem(key: string) {
    this.items.delete(key)
  }

  setItem(key: string, value: string) {
    this.items.set(key, String(value))
  }
}

const AUTH_RESPONSE = {
  user: {
    id: 1,
    username: 'test',
    email: 'test@example.com',
    role: 'USER',
    status: 'ACTIVE',
    mustChangePassword: false,
  },
  tokens: {
    accessToken: 'test-token',
    refreshToken: 'test-refresh',
    tokenType: 'Bearer',
    expiresIn: 3600,
  },
}

let storage: MemoryStorage

function installTestGlobals() {
  storage = new MemoryStorage()
  vi.stubGlobal('window', {
    location: {
      origin: 'http://localhost',
      pathname: '/',
      search: '',
      href: 'http://localhost/',
    },
    setTimeout,
    clearTimeout,
    localStorage: storage,
  })
  vi.stubGlobal('localStorage', storage)
}

function seedAuthenticatedState() {
  const authStore = useAuthStore()
  authStore.setAuth({
    accessToken: AUTH_RESPONSE.tokens.accessToken,
    refreshToken: AUTH_RESPONSE.tokens.refreshToken,
    user: AUTH_RESPONSE.user,
  })
  return authStore
}

beforeEach(() => {
  setActivePinia(createPinia())
  installTestGlobals()
  vi.clearAllMocks()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('Auth Store - Session State Machine', () => {
  it('setSessionExpired 會保留過期原因並清除登入狀態', () => {
    const authStore = seedAuthenticatedState()

    authStore.setSessionExpired('登入狀態已過期')

    expect(authStore.sessionExpiredReason).toBe('登入狀態已過期')
    expect(authStore.accessToken).toBeNull()
    expect(authStore.refreshToken).toBeNull()
    expect(authStore.user).toBeNull()
    expect(localStorage.getItem('docflow.accessToken')).toBeNull()
    expect(localStorage.getItem('docflow.refreshToken')).toBeNull()
    expect(localStorage.getItem('docflow.user')).toBeNull()
  })

  it('setSessionExpired 未提供原因時會使用預設訊息', () => {
    const authStore = useAuthStore()

    authStore.setSessionExpired()

    expect(authStore.sessionExpiredReason).toBe('登入狀態已過期，請重新登入。')
  })

  it('logout 會清空過期提示與登入狀態', async () => {
    const authStore = seedAuthenticatedState()
    authStore.sessionExpiredReason = '登入狀態已過期'
    vi.mocked(logoutApi).mockResolvedValueOnce(undefined)

    await authStore.logout()

    expect(logoutApi).toHaveBeenCalledWith({
      refreshToken: AUTH_RESPONSE.tokens.refreshToken,
      accessToken: AUTH_RESPONSE.tokens.accessToken,
    })
    expect(authStore.sessionExpiredReason).toBeNull()
    expect(authStore.accessToken).toBeNull()
    expect(authStore.refreshToken).toBeNull()
    expect(authStore.user).toBeNull()
  })

  it('login 成功後會清除過期提示', async () => {
    const authStore = useAuthStore()
    authStore.sessionExpiredReason = '登入狀態已過期'
    vi.mocked(loginApi).mockResolvedValueOnce(AUTH_RESPONSE)

    await authStore.login({
      username: 'test',
      password: 'secret',
    })

    expect(loginApi).toHaveBeenCalledWith({
      username: 'test',
      password: 'secret',
    })
    expect(authStore.sessionExpiredReason).toBeNull()
    expect(authStore.isAuthenticated).toBe(true)
  })

  it('register 成功後會清除過期提示', async () => {
    const authStore = useAuthStore()
    authStore.sessionExpiredReason = '登入狀態已過期'
    vi.mocked(registerApi).mockResolvedValueOnce(AUTH_RESPONSE)

    await authStore.register({
      username: 'test',
      email: 'test@example.com',
      password: 'secret',
    })

    expect(registerApi).toHaveBeenCalledWith({
      username: 'test',
      email: 'test@example.com',
      password: 'secret',
    })
    expect(authStore.sessionExpiredReason).toBeNull()
    expect(authStore.isAuthenticated).toBe(true)
  })

  it('bootstrapAuth 失敗時會清除登入狀態', async () => {
    const authStore = seedAuthenticatedState()
    vi.spyOn(authStore, 'bootstrapSession').mockRejectedValueOnce(new Error('bootstrap failed'))

    await authStore.bootstrapAuth()

    expect(authStore.accessToken).toBeNull()
    expect(authStore.refreshToken).toBeNull()
    expect(authStore.user).toBeNull()
    expect(authStore.sessionExpiredReason).toBeNull()
    expect(authStore.initialized).toBe(true)
    expect(localStorage.getItem('docflow.accessToken')).toBeNull()
    expect(localStorage.getItem('docflow.refreshToken')).toBeNull()
    expect(localStorage.getItem('docflow.user')).toBeNull()
  })

  it('bootstrapAuth 失敗時若已有過期原因，會保留提示訊息', async () => {
    const authStore = seedAuthenticatedState()
    authStore.sessionExpiredReason = '登入狀態已過期，請重新登入。'
    vi.spyOn(authStore, 'bootstrapSession').mockRejectedValueOnce(new Error('bootstrap failed'))

    await authStore.bootstrapAuth()

    expect(authStore.sessionExpiredReason).toBe('登入狀態已過期，請重新登入。')
    expect(authStore.accessToken).toBeNull()
    expect(authStore.refreshToken).toBeNull()
    expect(authStore.user).toBeNull()
  })
})
