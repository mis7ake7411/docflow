import { createPinia, setActivePinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { getCurrentUser, refreshToken as refreshTokenApi } from '@/features/auth/api'
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

function seedAuthState() {
  localStorage.setItem('docflow.refreshToken', 'old-refresh-token')
  localStorage.setItem('docflow.accessToken', 'old-access-token')
  localStorage.setItem(
    'docflow.user',
    JSON.stringify({
      id: 1,
      username: 'test',
      email: 'test@example.com',
      role: 'USER',
      status: 'ACTIVE',
      mustChangePassword: false,
    }),
  )
}

beforeEach(() => {
  setActivePinia(createPinia())
  installTestGlobals()
  vi.clearAllMocks()
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('Auth Store - Refresh Token Rotation', () => {
  it('refreshAccessToken ?ђе?еѕЊж??Њж­Ґ?ґж–°?°з? refresh token', async () => {
    seedAuthState()
    vi.mocked(refreshTokenApi).mockResolvedValueOnce({
      accessToken: 'new-access-token',
      refreshToken: 'new-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    })

    const authStore = useAuthStore()
    const accessToken = await authStore.refreshAccessToken()

    expect(accessToken).toBe('new-access-token')
    expect(authStore.accessToken).toBe('new-access-token')
    expect(authStore.refreshToken).toBe('new-refresh-token')
    expect(localStorage.getItem('docflow.accessToken')).toBe('new-access-token')
    expect(localStorage.getItem('docflow.refreshToken')).toBe('new-refresh-token')
  })

  it('bootstrapSession refresh ?ђе?еѕЊж??Ѓд??–ж???refresh token', async () => {
    localStorage.setItem('docflow.refreshToken', 'bootstrap-refresh-token')
    vi.mocked(refreshTokenApi).mockResolvedValueOnce({
      accessToken: 'bootstrapped-access-token',
      refreshToken: 'bootstrapped-refresh-token',
      tokenType: 'Bearer',
      expiresIn: 3600,
    })
    vi.mocked(getCurrentUser).mockResolvedValueOnce({
      id: 1,
      username: 'test',
      email: 'test@example.com',
      role: 'USER',
      status: 'ACTIVE',
      mustChangePassword: false,
    })

    const authStore = useAuthStore()
    await authStore.bootstrapSession()

    expect(authStore.accessToken).toBe('bootstrapped-access-token')
    expect(authStore.refreshToken).toBe('bootstrapped-refresh-token')
    expect(localStorage.getItem('docflow.accessToken')).toBe('bootstrapped-access-token')
    expect(localStorage.getItem('docflow.refreshToken')).toBe('bootstrapped-refresh-token')
  })
})
