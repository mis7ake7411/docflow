const STATUS_LABELS: Record<string, string> = {
  DRAFT: '草稿',
  ACTIVE: '啟用',
  INACTIVE: '停用',
  ARCHIVED: '封存',
}

const ROLE_LABELS: Record<string, string> = {
  USER: '一般使用者',
  ADMIN: '管理員',
  MANAGER: '主管',
}

const TARGET_TYPE_LABELS: Record<string, string> = {
  DOCUMENT: '文件',
  FOLDER: '資料夾',
  USER: '使用者',
  AUTH: '驗證',
}

const ACTION_LABELS: Record<string, string> = {
  CREATE: '建立',
  UPDATE: '更新',
  DELETE: '刪除',
  LOGIN: '登入',
  LOGOUT: '登出',
  REFRESH: '更新令牌',
  DOWNLOAD: '下載',
  UPLOAD: '上傳',
  VIEW: '檢視',
  SHARE_CREATE: '新增分享',
  SHARE_UPDATE: '更新分享',
  SHARE_DELETE: '取消分享',
}

const ACCESS_LEVEL_LABELS: Record<string, string> = {
  OWNER: '擁有者',
  VIEW: '可檢視',
  EDIT: '可編輯',
  ADMIN: '管理權限',
}

type ActivityDetail = Record<string, unknown>

export function getStatusLabel(status: string | null | undefined) {
  if (!status) return '未設定'
  return STATUS_LABELS[status] ?? status
}

export function getRoleLabel(role: string | null | undefined) {
  if (!role) return '訪客'
  return ROLE_LABELS[role] ?? role
}

export function getTargetTypeLabel(targetType: string | null | undefined) {
  if (!targetType) return '未知'
  return TARGET_TYPE_LABELS[targetType] ?? targetType
}

export function getActionLabel(action: string | null | undefined) {
  if (!action) return '未知'
  return ACTION_LABELS[action] ?? action
}

export function getAccessLevelLabel(accessLevel: string | null | undefined) {
  if (!accessLevel) return '未設定'
  return ACCESS_LEVEL_LABELS[accessLevel] ?? accessLevel
}

export function formatActivityDetail(action: string | null | undefined, detailJson: string | null | undefined) {
  if (!detailJson) return '—'

  const parsed = parseActivityDetail(detailJson)
  if (!parsed) {
    return detailJson
  }

  switch (action) {
    case 'SHARE_CREATE':
      return `${resolveUserLabel(parsed)} 取得 ${resolvePermissionLabel(parsed.permission)} 權限`
    case 'SHARE_UPDATE': {
      const userLabel = resolveUserLabel(parsed)
      const nextPermission = resolvePermissionLabel(parsed.permission)
      const previousPermission = resolvePermissionLabel(parsed.previousPermission)

      if (parsed.previousPermission) {
        return `${userLabel} 權限由 ${previousPermission} 調整為 ${nextPermission}`
      }
      return `${userLabel} 權限更新為 ${nextPermission}`
    }
    case 'SHARE_DELETE':
      return `已取消 ${resolveUserLabel(parsed)} 的分享權限`
    default:
      return formatGenericDetail(parsed)
  }
}

function parseActivityDetail(detailJson: string): ActivityDetail | null {
  try {
    const parsed = JSON.parse(detailJson) as unknown
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return null
    }
    return parsed as ActivityDetail
  } catch {
    return null
  }
}

function resolveUserLabel(detail: ActivityDetail) {
  const username = detail.sharedWithUsername
  if (typeof username === 'string' && username.trim()) {
    return username
  }

  const userId = detail.sharedWithUserId
  if (typeof userId === 'number' || typeof userId === 'string') {
    return `使用者 #${userId}`
  }

  return '該使用者'
}

function resolvePermissionLabel(permission: unknown) {
  if (typeof permission !== 'string' || !permission.trim()) {
    return '未設定'
  }
  return getAccessLevelLabel(permission)
}

function formatGenericDetail(detail: ActivityDetail) {
  const entries = Object.entries(detail)
    .filter(([, value]) => value !== null && value !== undefined && value !== '')
    .map(([key, value]) => `${key}: ${String(value)}`)

  return entries.length ? entries.join('、') : '—'
}
