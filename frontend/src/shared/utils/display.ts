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
  REFRESH: '刷新權杖',
  DOWNLOAD: '下載',
  UPLOAD: '上傳',
  VIEW: '查看',
}

export function getStatusLabel(status: string | null | undefined) {
  if (!status) {
    return '未設定'
  }

  return STATUS_LABELS[status] ?? status
}

export function getRoleLabel(role: string | null | undefined) {
  if (!role) {
    return '訪客'
  }

  return ROLE_LABELS[role] ?? role
}

export function getTargetTypeLabel(targetType: string | null | undefined) {
  if (!targetType) {
    return '未知'
  }

  return TARGET_TYPE_LABELS[targetType] ?? targetType
}

export function getActionLabel(action: string | null | undefined) {
  if (!action) {
    return '未知'
  }

  return ACTION_LABELS[action] ?? action
}
