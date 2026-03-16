import { defineStore } from 'pinia'
import { login, logout, refreshToken, type LoginRequest } from '@/features/auth/api'
import type { UserSummary } from '@/shared/types/auth'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  user: UserSummary | null
}

const ACCESS_TOKEN_KEY = 'docflow.accessToken'
const REFRESH_TOKEN_KEY = 'docflow.refreshToken'
const USER_KEY = 'docflow.user'

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem(ACCESS_TOKEN_KEY),
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY),
    user: localStorage.getItem(USER_KEY) ? JSON.parse(localStorage.getItem(USER_KEY) as string) : null,
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
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
      }
    },
  },
})
