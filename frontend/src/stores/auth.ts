import { defineStore } from 'pinia'

interface UserSummary {
  id: number
  username: string
  email: string
  role: string
  status: string
}

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
  },
})
