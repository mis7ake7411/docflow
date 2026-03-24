import type { UserSummary } from '@/shared/types/auth'

export const PERMISSION_MESSAGES = {
  folderHint: '僅能修改自己建立的資料夾',
  documentHint: '僅能修改自己建立的文件',
  folderForbidden: '無權限操作此資料夾',
  documentForbidden: '無權限操作此文件',
}

export function isAdminOrManager(user: UserSummary | null) {
  return user?.role === 'ADMIN' || user?.role === 'MANAGER'
}

export function canModifyResource(createdBy: number | null | undefined, user: UserSummary | null) {
  if (isAdminOrManager(user)) {
    return true
  }
  if (!user || createdBy == null) {
    return false
  }
  return createdBy === user.id
}
