let accessTokenGetter: (() => string | null) | null = null
let refreshHandler: (() => Promise<string | null>) | null = null
let logoutHandler: ((reason?: string) => void) | null = null

export function registerAuthBridge(options: {
  getAccessToken: () => string | null
  refreshAccessToken: () => Promise<string | null>
  onLogout: (reason?: string) => void
}) {
  accessTokenGetter = options.getAccessToken
  refreshHandler = options.refreshAccessToken
  logoutHandler = options.onLogout
}

export function getAccessTokenFromBridge(): string | null {
  return accessTokenGetter ? accessTokenGetter() : null
}

export async function refreshAccessTokenFromBridge(): Promise<string | null> {
  return refreshHandler ? refreshHandler() : null
}

export function logoutFromBridge(reason?: string) {
  if (logoutHandler) {
    logoutHandler(reason)
  }
}
