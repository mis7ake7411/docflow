import axios from 'axios'
import { getAccessTokenFromBridge, logoutFromBridge, refreshAccessTokenFromBridge } from '@/shared/api/auth-bridge'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || window.location.origin

export const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

apiClient.interceptors.request.use((config) => {
  const token = getAccessTokenFromBridge()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest?._retry) {
      originalRequest._retry = true
      const newAccessToken = await refreshAccessTokenFromBridge()

      if (newAccessToken) {
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
        return apiClient(originalRequest)
      }

      // Token 刷新失敗或 token 已失效，執行登出並導回登入頁
      logoutFromBridge()

      // 保存當前路徑以供登入後重定向
      const currentPath = window.location.pathname + window.location.search
      if (currentPath !== '/login' && currentPath !== '/register') {
        const redirectUrl = `/login?redirect=${encodeURIComponent(currentPath)}`
        window.location.href = redirectUrl
      }
    }

    return Promise.reject(error)
  },
)
