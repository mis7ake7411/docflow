import { apiClient } from '@/shared/api/axios'

export interface LoginRequest {
  username: string
  password: string
}

export interface UserSummary {
  id: number
  username: string
  email: string
  role: string
  status: string
}

export interface AuthTokens {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
}

export interface AuthResponse {
  user: UserSummary
  tokens: AuthTokens
}

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export interface RefreshRequest {
  refreshToken: string
}

export interface LogoutRequest {
  refreshToken: string
  accessToken?: string
}

export async function login(request: LoginRequest): Promise<AuthResponse> {
  const response = await apiClient.post<ApiResponse<AuthResponse>>('/api/auth/login', request)
  return response.data.data
}

export async function refreshToken(request: RefreshRequest): Promise<AuthTokens> {
  const response = await apiClient.post<ApiResponse<AuthTokens>>('/api/auth/refresh', request)
  return response.data.data
}

export async function getCurrentUser(): Promise<UserSummary> {
  const response = await apiClient.get<ApiResponse<UserSummary>>('/api/auth/me')
  return response.data.data
}

export async function logout(request: LogoutRequest): Promise<void> {
  await apiClient.post('/api/auth/logout', request)
}
