import { defineStore } from 'pinia'
import {
  changePassword,
  getCurrentUser,
  login,
  logout,
  refreshToken,
  register,
  type ChangePasswordRequest,
  type LoginRequest,
  type RegisterRequest,
} from '@/features/auth/api'
import type { UserSummary } from '@/shared/types/auth'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserSummary | null
  initialized: boolean
}

const ACCESS_TOKEN_KEY = 'docflow.accessToken'
const REFRESH_TOKEN_KEY = 'docflow.refreshToken'
const USER_KEY = 'docflow.user'
const BOOTSTRAP_TIMEOUT_MS = 12000

function withTimeout<T>(promise: Promise<T>, timeoutMs: number) {
  return new Promise<T>((resolve, reject) => {
    const timeoutId = window.setTimeout(() => {
      reject(new Error('auth bootstrap timeout'))
    }, timeoutMs)

    promise
      .then((value) => {
        window.clearTimeout(timeoutId)
        resolve(value)
      })
      .catch((error) => {
        window.clearTimeout(timeoutId)
        reject(error)
      })
  })
}

function readUserFromStorage(): UserSummary | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as UserSummary
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem(ACCESS_TOKEN_KEY),
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
    user: readUserFromStorage(),
    initialized: false,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken && state.user),
    userRole: (state) => state.user?.role ?? null,
  },
  actions: {
    setAuth(payload: { accessToken: string; refreshToken: string; user: UserSummary }) {
      this.accessToken = payload.accessToken
      this.refreshToken = payload.refreshToken
      this.user = payload.user

      localStorage.setItem(ACCESS_TOKEN_KEY, payload.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, payload.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(payload.user))
    },
    clearAuth() {
      this.accessToken = null
      this.refreshToken = null
      this.user = null

      localStorage.removeItem(ACCESS_TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
    async login(payload: LoginRequest) {
      const response = await login(payload)
      this.setAuth({
        accessToken: response.tokens.accessToken,
        refreshToken: response.tokens.refreshToken,
        user: response.user,
      })
      this.initialized = true
    },
    async register(payload: RegisterRequest) {
      const response = await register(payload)
      this.setAuth({
        accessToken: response.tokens.accessToken,
        refreshToken: response.tokens.refreshToken,
        user: response.user,
      })
      this.initialized = true
    },
    async refreshAccessToken(): Promise<string | null> {
      if (!this.refreshToken) {
        this.clearAuth()
        return null
      }

      try {
        const tokens = await refreshToken({ refreshToken: this.refreshToken })
        this.accessToken = tokens.accessToken
        localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
        return tokens.accessToken
      } catch (error) {
        this.clearAuth()
        return null
      }
    },
    async bootstrapAuth() {
      if (this.initialized) {
        return
      }

      try {
        await withTimeout(this.bootstrapSession(), BOOTSTRAP_TIMEOUT_MS)
      } catch (error) {
        this.clearAuth()
      } finally {
        this.initialized = true
      }
    },
    async logout() {
      try {
        if (this.refreshToken) {
          await logout({
            refreshToken: this.refreshToken,
            accessToken: this.accessToken || undefined,
          })
        }
      } finally {
        this.clearAuth()
        this.initialized = true
      }
    },
    async changePassword(payload: ChangePasswordRequest) {
      await changePassword(payload)
      if (this.user) {
        this.user = {
          ...this.user,
          mustChangePassword: false,
        }
        localStorage.setItem(USER_KEY, JSON.stringify(this.user))
      }
    },
    async bootstrapSession() {
      if (!this.accessToken && this.refreshToken) {
        const tokens = await refreshToken({ refreshToken: this.refreshToken })
        this.accessToken = tokens.accessToken
        localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
      }

      if (!this.accessToken) {
        this.clearAuth()
        return
      }

      const user = await getCurrentUser()
      this.user = user
      localStorage.setItem(USER_KEY, JSON.stringify(user))
    },
  },
})
