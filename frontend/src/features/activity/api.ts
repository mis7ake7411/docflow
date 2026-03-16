import { apiClient } from '@/shared/api/axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
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

export async function getActivities(): Promise<ActivityLogItem[]> {
  const response = await apiClient.get<ApiResponse<ActivityLogItem[]>>('/api/activities')
  return response.data.data
}
