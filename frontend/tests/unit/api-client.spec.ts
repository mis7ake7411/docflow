import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

const authBridge = vi.hoisted(() => ({
  getAccessTokenFromBridge: vi.fn(),
  logoutFromBridge: vi.fn(),
  refreshAccessTokenFromBridge: vi.fn(),
}))

vi.mock('@/shared/api/auth-bridge', () => authBridge)

function createDeferred<T>() {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void

  const promise = new Promise<T>((res, rej) => {
    resolve = res
    reject = rej
  })

  return { promise, resolve, reject }
}

function getAuthorizationHeader(headers: unknown) {
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

function createStorage(store: Map<string, string>) {
  return {
    getItem: (key: string) => store.get(key) ?? null,
    setItem: (key: string, value: string) => {
      store.set(key, value)
    },
    removeItem: (key: string) => {
      store.delete(key)
    },
  }
}

function installWindow(pathname: string, search: string) {
  const hrefSetter = vi.fn()
  const localStorageStore = new Map<string, string>()
  const sessionStorageStore = new Map<string, string>()

  vi.stubGlobal('window', {
    location: {
      origin: 'http://localhost',
      pathname,
      search,
      get href() {
        return `http://localhost${pathname}${search}`
      },
      set href(value: string) {
        hrefSetter(value)
      },
    },
    clearTimeout,
    localStorage: createStorage(localStorageStore),
    sessionStorage: createStorage(sessionStorageStore),
    setTimeout,
  })

  return { hrefSetter, localStorageStore, sessionStorageStore }
}

async function loadApiClient() {
  vi.resetModules()
  return await import('@/shared/api/axios')
}

beforeEach(() => {
  vi.clearAllMocks()
  authBridge.getAccessTokenFromBridge.mockReturnValue('old-token')
  authBridge.logoutFromBridge.mockReturnValue(true)
  installWindow('/documents', '?page=1')
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('apiClient 401 refresh flow', () => {
  it('多個 401 請求會共用同一次 refresh，並用新 token 重送', async () => {
    const refreshDeferred = createDeferred<string | null>()
    const observedAuthorizationHeaders: string[] = []

    authBridge.getAccessTokenFromBridge.mockReturnValue('old-token')
    authBridge.refreshAccessTokenFromBridge.mockImplementation(() => refreshDeferred.promise)

    const { apiClient } = await loadApiClient()
    const adapter = vi.fn(async (config: any) => {
      observedAuthorizationHeaders.push(getAuthorizationHeader(config.headers) ?? 'missing')

      if (adapter.mock.calls.length <= 2) {
        return Promise.reject({
          config,
          response: { status: 401 },
        })
      }

      return {
        config,
        data: { ok: true },
        headers: {},
        status: 200,
        statusText: 'OK',
      }
    })

    apiClient.defaults.adapter = adapter

    const requestA = apiClient.get('/api/documents')
    const requestB = apiClient.get('/api/folders')

    await vi.waitFor(() => {
      expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(1)
    })

    refreshDeferred.resolve('new-token')

    await expect(requestA).resolves.toMatchObject({ status: 200 })
    await expect(requestB).resolves.toMatchObject({ status: 200 })

    expect(adapter).toHaveBeenCalledTimes(4)
    expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(1)
    expect(observedAuthorizationHeaders.slice(0, 2)).toEqual([
      'Bearer old-token',
      'Bearer old-token',
    ])
    expect(observedAuthorizationHeaders.slice(2)).toEqual([
      'Bearer new-token',
      'Bearer new-token',
    ])
  })

  it('refresh 回傳 null 時只會登出與導頁一次', async () => {
    const refreshDeferred = createDeferred<string | null>()

    authBridge.refreshAccessTokenFromBridge.mockReturnValue(refreshDeferred.promise)

    const { hrefSetter } = installWindow('/documents', '?page=1')
    const { apiClient } = await loadApiClient()
    const adapter = vi.fn(async (config: any) => {
      return Promise.reject({
        config,
        response: { status: 401 },
      })
    })

    apiClient.defaults.adapter = adapter

    const requestA = apiClient.get('/api/documents')
    const requestB = apiClient.get('/api/folders')

    await vi.waitFor(() => {
      expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(1)
    })

    refreshDeferred.resolve(null)

    await expect(requestA).rejects.toMatchObject({ response: { status: 401 } })
    await expect(requestB).rejects.toMatchObject({ response: { status: 401 } })

    expect(authBridge.logoutFromBridge).toHaveBeenCalledTimes(1)
    expect(hrefSetter).toHaveBeenCalledTimes(1)
    expect(hrefSetter).toHaveBeenCalledWith('/login?redirect=%2Fdocuments%3Fpage%3D1')
  })

  it('staggered 401 在 refresh 完成前仍會共用同一波 refresh', async () => {
    const refreshDeferred = createDeferred<string | null>()
    const firstRequestDeferred = createDeferred<void>()
    const secondRequestDeferred = createDeferred<void>()
    const observedAuthorizationHeaders: string[] = []

    authBridge.getAccessTokenFromBridge.mockReturnValue('old-token')
    authBridge.refreshAccessTokenFromBridge.mockImplementation(() => refreshDeferred.promise)

    const { apiClient } = await loadApiClient()
    const adapter = vi.fn(async (config: any) => {
      observedAuthorizationHeaders.push(getAuthorizationHeader(config.headers) ?? 'missing')

      if (adapter.mock.calls.length === 1) {
        await firstRequestDeferred.promise
        return Promise.reject({
          config,
          response: { status: 401 },
        })
      }

      if (adapter.mock.calls.length === 2) {
        await secondRequestDeferred.promise
        return Promise.reject({
          config,
          response: { status: 401 },
        })
      }

      return {
        config,
        data: { ok: true },
        headers: {},
        status: 200,
        statusText: 'OK',
      }
    })

    apiClient.defaults.adapter = adapter

    const requestA = apiClient.get('/api/documents')
    const requestB = apiClient.get('/api/folders')

    firstRequestDeferred.resolve()

    await vi.waitFor(() => {
      expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(1)
    })

    refreshDeferred.resolve('new-token')
    secondRequestDeferred.resolve()

    await expect(requestA).resolves.toMatchObject({ status: 200 })
    await expect(requestB).resolves.toMatchObject({ status: 200 })

    expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(1)
    expect(observedAuthorizationHeaders.slice(0, 2)).toEqual([
      'Bearer old-token',
      'Bearer old-token',
    ])
    expect(observedAuthorizationHeaders.slice(2)).toEqual([
      'Bearer new-token',
      'Bearer new-token',
    ])
  })

  it('refresh 進行中才送出的新 401 請求，仍會等待同一次 refresh', async () => {
    const refreshDeferred = createDeferred<string | null>()
    const firstRequestDeferred = createDeferred<void>()
    const observedAuthorizationHeaders: string[] = []

    authBridge.getAccessTokenFromBridge.mockReturnValue('old-token')
    authBridge.refreshAccessTokenFromBridge.mockImplementation(() => refreshDeferred.promise)

    const { apiClient } = await loadApiClient()
    const adapter = vi.fn(async (config: any) => {
      observedAuthorizationHeaders.push(getAuthorizationHeader(config.headers) ?? 'missing')

      if (adapter.mock.calls.length === 1) {
        await firstRequestDeferred.promise
        return Promise.reject({
          config,
          response: { status: 401 },
        })
      }

      if (adapter.mock.calls.length === 2) {
        return Promise.reject({
          config,
          response: { status: 401 },
        })
      }

      return {
        config,
        data: { ok: true },
        headers: {},
        status: 200,
        statusText: 'OK',
      }
    })

    apiClient.defaults.adapter = adapter

    const requestA = apiClient.get('/api/documents')

    firstRequestDeferred.resolve()

    await vi.waitFor(() => {
      expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(1)
    })

    const requestB = apiClient.get('/api/folders')
    refreshDeferred.resolve('new-token')

    await expect(requestA).resolves.toMatchObject({ status: 200 })
    await expect(requestB).resolves.toMatchObject({ status: 200 })

    expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(1)
    expect(observedAuthorizationHeaders).toEqual([
      'Bearer old-token',
      'Bearer old-token',
      'Bearer new-token',
      'Bearer new-token',
    ])
  })

  it('refresh promise 直接 reject 時也只會登出與導頁一次', async () => {
    authBridge.refreshAccessTokenFromBridge.mockRejectedValueOnce(new Error('refresh failed'))

    const { hrefSetter } = installWindow('/documents', '?page=1')
    const { apiClient } = await loadApiClient()
    const adapter = vi.fn(async (config: any) => {
      return Promise.reject({
        config,
        response: { status: 401 },
      })
    })

    apiClient.defaults.adapter = adapter

    const requestA = apiClient.get('/api/documents')
    const requestB = apiClient.get('/api/folders')
    const requestAError = requestA.catch((error) => error)
    const requestBError = requestB.catch((error) => error)

    await vi.waitFor(() => {
      expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(1)
    })

    await expect(requestAError).resolves.toMatchObject({ response: { status: 401 } })
    await expect(requestBError).resolves.toMatchObject({ response: { status: 401 } })

    expect(authBridge.logoutFromBridge).toHaveBeenCalledTimes(1)
    expect(hrefSetter).toHaveBeenCalledTimes(1)
    expect(hrefSetter).toHaveBeenCalledWith('/login?redirect=%2Fdocuments%3Fpage%3D1')
  })

  it('同一 token 再次失效時會開啟新一波 refresh', async () => {
    const firstRefreshDeferred = createDeferred<string | null>()
    const secondRefreshDeferred = createDeferred<string | null>()
    const observedAuthorizationHeaders: string[] = []

    authBridge.getAccessTokenFromBridge.mockReturnValue('old-token')
    authBridge.refreshAccessTokenFromBridge
      .mockImplementationOnce(() => firstRefreshDeferred.promise)
      .mockImplementationOnce(() => secondRefreshDeferred.promise)

    const { apiClient } = await loadApiClient()
    const adapter = vi.fn(async (config: any) => {
      observedAuthorizationHeaders.push(getAuthorizationHeader(config.headers) ?? 'missing')

      if (adapter.mock.calls.length === 1) {
        return Promise.reject({
          config,
          response: { status: 401 },
        })
      }

      if (adapter.mock.calls.length === 2) {
        return {
          config,
          data: { ok: true },
          headers: {},
          status: 200,
          statusText: 'OK',
        }
      }

      if (adapter.mock.calls.length === 3) {
        return Promise.reject({
          config,
          response: { status: 401 },
        })
      }

      return {
        config,
        data: { ok: true },
        headers: {},
        status: 200,
        statusText: 'OK',
      }
    })

    apiClient.defaults.adapter = adapter

    const requestA = apiClient.get('/api/documents')

    await vi.waitFor(() => {
      expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(1)
    })

    firstRefreshDeferred.resolve('same-token')

    await expect(requestA).resolves.toMatchObject({ status: 200 })

    const requestB = apiClient.get('/api/folders')

    await vi.waitFor(() => {
      expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(2)
    })

    secondRefreshDeferred.resolve('same-token')

    await expect(requestB).resolves.toMatchObject({ status: 200 })

    expect(authBridge.refreshAccessTokenFromBridge).toHaveBeenCalledTimes(2)
    expect(observedAuthorizationHeaders).toEqual([
      'Bearer old-token',
      'Bearer same-token',
      'Bearer old-token',
      'Bearer same-token',
    ])
  })

  it('bridge 尚未註冊時會直接清除本地登入狀態並保留過期訊息', async () => {
    authBridge.logoutFromBridge.mockReturnValue(false)
    authBridge.refreshAccessTokenFromBridge.mockResolvedValueOnce(null)

    const { hrefSetter, localStorageStore, sessionStorageStore } = installWindow('/app', '')
    localStorageStore.set('docflow.accessToken', 'expired-token')
    localStorageStore.set('docflow.refreshToken', 'expired-refresh-token')
    localStorageStore.set('docflow.user', '{"id":1}')

    const { apiClient } = await loadApiClient()
    const adapter = vi.fn(async (config: any) => {
      return Promise.reject({
        config,
        response: { status: 401 },
      })
    })

    apiClient.defaults.adapter = adapter

    await expect(apiClient.get('/api/auth/me')).rejects.toMatchObject({ response: { status: 401 } })

    expect(authBridge.logoutFromBridge).toHaveBeenCalledWith('登入狀態已過期，請重新登入。')
    expect(localStorageStore.get('docflow.accessToken')).toBeUndefined()
    expect(localStorageStore.get('docflow.refreshToken')).toBeUndefined()
    expect(localStorageStore.get('docflow.user')).toBeUndefined()
    expect(sessionStorageStore.get('docflow.sessionExpiredReason')).toBe('登入狀態已過期，請重新登入。')
    expect(hrefSetter).toHaveBeenCalledWith('/login?redirect=%2Fapp')
  })

  it('refresh 端點本身回 401 時不會遞迴觸發下一次 refresh', async () => {
    const { apiClient } = await loadApiClient()
    const adapter = vi.fn(async (config: any) => {
      return Promise.reject({
        config,
        response: { status: 401 },
      })
    })

    apiClient.defaults.adapter = adapter

    await expect(apiClient.post('/api/auth/refresh', { refreshToken: 'expired' })).rejects.toMatchObject({
      response: { status: 401 },
    })

    expect(authBridge.refreshAccessTokenFromBridge).not.toHaveBeenCalled()
    expect(authBridge.logoutFromBridge).not.toHaveBeenCalled()
  })
})
