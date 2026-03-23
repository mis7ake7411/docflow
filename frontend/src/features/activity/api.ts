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

export interface ActivityLogItem {
  id: number
  userId: number | null
  targetType: string
  targetId: number | null
  action: string
  detailJson: string | null
  createdAt: string
}

export async function getActivities(page = 0, size = 10): Promise<PagedResponse<ActivityLogItem>> {
  const response = await apiClient.get<ApiResponse<PagedResponse<ActivityLogItem>>>('/api/activities', {
    params: { page, size },
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
