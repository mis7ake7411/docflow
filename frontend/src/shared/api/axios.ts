import axios from 'axios'
import { getAccessTokenFromBridge, logoutFromBridge, refreshAccessTokenFromBridge } from '@/shared/api/auth-bridge'

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
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

      logoutFromBridge()
    }

    return Promise.reject(error)
  },
)
