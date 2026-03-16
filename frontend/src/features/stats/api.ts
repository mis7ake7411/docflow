import { apiClient } from '@/shared/api/axios'

export interface ApiResponse<T> {
  success: boolean
  data: T
  message: string | null
}

export interface HotDocumentItem {
  documentId: number
  title: string
  status: string
  score: number
}

export interface RecentViewItem {
  documentId: number
  title: string
  status: string
  score: number
}

export async function getHotDocuments(): Promise<HotDocumentItem[]> {
  const response = await apiClient.get<ApiResponse<HotDocumentItem[]>>('/api/stats/hot-documents')
  return response.data.data
}

export async function getRecentViews(): Promise<RecentViewItem[]> {
  const response = await apiClient.get<ApiResponse<RecentViewItem[]>>('/api/users/me/recent-views')
  return response.data.data
}
