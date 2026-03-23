import { apiClient } from '@/shared/api/axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export interface PagedResponse<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface UserListItem {
  id: number
  username: string
  email: string
  role: string
  status: string
  createdAt: string
}

export interface CreateUserRequest {
  username: string
  email: string
  role: string
  status: string
}

export interface UpdateUserRequest {
  role: string
  status: string
}

export interface CreateUserResponse {
  user: UserListItem
  tempPassword: string
}

export async function getUsers(page = 0, size = 10, keyword?: string): Promise<PagedResponse<UserListItem>> {
  const response = await apiClient.get<ApiResponse<PagedResponse<UserListItem>>>('/api/users', {
    params: {
      page,
      size,
      keyword: keyword || undefined,
    },
  })
  const payload = response.data.data
  if (!payload) {
    return {
      items: [],
      page: 0,
      size,
      totalElements: 0,
      totalPages: 0,
    }
  }
  return {
    items: payload.items ?? [],
    page: payload.page ?? 0,
    size: payload.size ?? size,
    totalElements: payload.totalElements ?? 0,
    totalPages: payload.totalPages ?? 0,
  }
}

export async function createUser(request: CreateUserRequest): Promise<CreateUserResponse> {
  const response = await apiClient.post<ApiResponse<CreateUserResponse>>('/api/users', request)
  return response.data.data
}

export async function updateUser(id: number, request: UpdateUserRequest): Promise<UserListItem> {
  const response = await apiClient.put<ApiResponse<UserListItem>>(`/api/users/${id}`, request)
  return response.data.data
}
