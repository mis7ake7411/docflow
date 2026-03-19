import type { AppRole } from '@/router/routes'

export function hasAnyRole(userRole: string | undefined, allowedRoles?: AppRole[]) {
  if (!allowedRoles || allowedRoles.length === 0) {
    return true
  }

  if (!userRole) {
    return false
  }

  return allowedRoles.includes(userRole as AppRole)
}
