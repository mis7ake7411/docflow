import type { DocumentAccessLevel } from '@/features/document/api'
import type { UserSummary } from '@/shared/types/auth'

export const PERMISSION_MESSAGES = {
  folderHint: '僅能修改自己建立的資料夾',
  documentHint: '僅能修改自己建立或被授權編輯的文件',
  documentReadOnly: '此文件僅授權檢視與下載',
  documentShareHint: '只有文件擁有者、管理員或主管可管理分享',
  folderForbidden: '無權限操作此資料夾',
  documentForbidden: '無權限操作此文件',
  documentShareForbidden: '無權限管理此文件分享',
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

export function canEditDocument(
  document: { createdBy: number | null | undefined; accessLevel?: DocumentAccessLevel | null },
  user: UserSummary | null,
) {
  if (canModifyResource(document.createdBy, user)) {
    return true
  }
  return document.accessLevel === 'EDIT'
}

export function canDeleteDocument(
  document: { createdBy: number | null | undefined },
  user: UserSummary | null,
) {
  return canModifyResource(document.createdBy, user)
}

export function canShareDocument(
  document: { createdBy: number | null | undefined },
  user: UserSummary | null,
) {
  return canModifyResource(document.createdBy, user)
}

export function getDocumentAccessHint(
  document: { accessLevel?: DocumentAccessLevel | null; createdBy: number | null | undefined },
  user: UserSummary | null,
) {
  if (canEditDocument(document, user)) {
    return ''
  }
  if (document.accessLevel === 'VIEW') {
    return PERMISSION_MESSAGES.documentReadOnly
  }
  return PERMISSION_MESSAGES.documentForbidden
}
