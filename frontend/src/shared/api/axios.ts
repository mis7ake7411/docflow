import axios from 'axios'
import { getAccessTokenFromBridge, logoutFromBridge, refreshAccessTokenFromBridge } from '@/shared/api/auth-bridge'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin
const ACCESS_TOKEN_KEY = 'docflow.accessToken'
const REFRESH_TOKEN_KEY = 'docflow.refreshToken'
const USER_KEY = 'docflow.user'
const SESSION_EXPIRED_REASON_KEY = 'docflow.sessionExpiredReason'
const DEFAULT_SESSION_EXPIRED_MESSAGE = '登入狀態已過期，請重新登入。'

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

type RefreshWave = {
  promise: Promise<string | null>
}

type AuthTrackedRequestConfig = {
  _retry?: boolean
  _authBatchId?: number
  url?: string
  headers: unknown
}

let currentRequestBatchId = 0
let activeRefreshBatchId: number | null = null
const pendingRequestsByBatchId = new Map<number, number>()
const refreshWavesByBatchId = new Map<number, RefreshWave>()
let authFailureHandlingPromise: Promise<void> | null = null

function clearPersistedAuthState() {
  window.localStorage?.removeItem(ACCESS_TOKEN_KEY)
  window.localStorage?.removeItem(REFRESH_TOKEN_KEY)
  window.localStorage?.removeItem(USER_KEY)
}

function getAuthorizationHeader(headers: unknown): string | null {
  if (!headers || typeof headers !== 'object') {
    return null
  }

  const record = headers as {
    get?: (key: string) => string | null
    Authorization?: string
    authorization?: string
  }

  if (typeof record.get === 'function') {
    return record.get('Authorization') ?? record.get('authorization')
  }

  return record.Authorization ?? record.authorization ?? null
}

function setAuthorizationHeader(headers: unknown, token: string) {
  const value = `Bearer ${token}`

  if (headers && typeof headers === 'object') {
    const record = headers as {
      set?: (key: string, value: string) => void
      Authorization?: string
      authorization?: string
    }

    if (typeof record.set === 'function') {
      record.set('Authorization', value)
      return
    }

    record.Authorization = value
  }
}

function getRequestBatchId(config: AuthTrackedRequestConfig): number {
  return config._authBatchId ?? 0
}

function setRequestBatchId(config: AuthTrackedRequestConfig, batchId: number) {
  config._authBatchId = batchId
}

function acquireRequestBatch(batchId: number) {
  pendingRequestsByBatchId.set(batchId, (pendingRequestsByBatchId.get(batchId) ?? 0) + 1)
}

function releaseRequestBatch(batchId: number) {
  const remaining = (pendingRequestsByBatchId.get(batchId) ?? 1) - 1

  if (remaining > 0) {
    pendingRequestsByBatchId.set(batchId, remaining)
    return
  }

  pendingRequestsByBatchId.delete(batchId)

  if (activeRefreshBatchId !== batchId) {
    refreshWavesByBatchId.delete(batchId)
  }
}

function getRefreshWave(batchId: number): RefreshWave {
  const existingWave = refreshWavesByBatchId.get(batchId)
  if (existingWave) {
    return existingWave
  }

  const wave: RefreshWave = {
    promise: Promise.resolve()
      .then(() => refreshAccessTokenFromBridge())
      .catch(() => null)
      .finally(() => {
        if (activeRefreshBatchId === batchId) {
          activeRefreshBatchId = null
          currentRequestBatchId = batchId + 1
        }
      }),
  }

  refreshWavesByBatchId.set(batchId, wave)
  activeRefreshBatchId = batchId

  return wave
}

apiClient.interceptors.request.use((config) => {
  const trackedConfig = config as AuthTrackedRequestConfig
  const batchId = activeRefreshBatchId ?? currentRequestBatchId

  setRequestBatchId(trackedConfig, batchId)
  acquireRequestBatch(batchId)

  const token = getAccessTokenFromBridge()
  if (token && !getAuthorizationHeader(config.headers)) {
    setAuthorizationHeader(config.headers, token)
  }

  return config
})

apiClient.interceptors.response.use(
  (response) => {
    releaseRequestBatch(getRequestBatchId(response.config as AuthTrackedRequestConfig))
    return response
  },
  async (error) => {
    const originalRequest = error.config as AuthTrackedRequestConfig
    const requestBatchId = getRequestBatchId(originalRequest)
    const requestUrl = originalRequest?.url ?? ''

    try {
      if (error.response?.status === 401 && requestUrl.includes('/api/auth/refresh')) {
        return Promise.reject(error)
      }

      if (error.response?.status === 401 && !originalRequest?._retry) {
        originalRequest._retry = true

        const refreshWave = getRefreshWave(requestBatchId)
        const newAccessToken = await refreshWave.promise

        if (newAccessToken) {
          setAuthorizationHeader(originalRequest.headers, newAccessToken)
          return apiClient(originalRequest as never)
        }

        if (!authFailureHandlingPromise) {
          authFailureHandlingPromise = (async () => {
            const handledByBridge = logoutFromBridge(DEFAULT_SESSION_EXPIRED_MESSAGE)

            if (!handledByBridge) {
              clearPersistedAuthState()
              window.sessionStorage?.setItem(SESSION_EXPIRED_REASON_KEY, DEFAULT_SESSION_EXPIRED_MESSAGE)
            }

            const currentPath = window.location.pathname + window.location.search
            const isLoginPath = currentPath === '/login' || currentPath.startsWith('/login?')
            const isRegisterPath = currentPath === '/register' || currentPath.startsWith('/register?')

            if (!isLoginPath && !isRegisterPath) {
              const redirectUrl = `/login?redirect=${encodeURIComponent(currentPath)}`
              window.location.href = redirectUrl
            }
          })().finally(() => {
            authFailureHandlingPromise = null
          })
        }

        await authFailureHandlingPromise
      }

      return Promise.reject(error)
    } finally {
      releaseRequestBatch(requestBatchId)
    }
  },
)
