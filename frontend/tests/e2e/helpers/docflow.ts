import { expect, type Page } from '@playwright/test'

const apiBaseUrl = process.env.DOCFLOW_E2E_API_BASE_URL ?? 'http://localhost:8080'

export type SharePermission = 'VIEW' | 'EDIT'

export type AccountPair = {
  ownerUser: string
  ownerPass: string
  otherUser: string
  otherPass: string
}

export async function login(page: Page, username: string, password: string) {
  await page.goto('/login')
  await page.getByRole('textbox', { name: '帳號' }).fill(username)
  await page.getByRole('textbox', { name: '密碼' }).fill(password)
  await page.getByRole('button', { name: '登入' }).click()
  await expect(page).toHaveURL(/\/app/)
}

export async function register(page: Page, username: string, password: string) {
  await page.goto('/login')
  await page.getByRole('link', { name: '註冊' }).click()
  await expect(page).toHaveURL(/\/register/)
  await page.getByRole('textbox', { name: '帳號' }).fill(username)
  await page.getByRole('textbox', { name: 'Email' }).fill(`${username}@example.com`)
  await page.getByRole('textbox', { name: '密碼' }).fill(password)
  await page.getByRole('button', { name: '建立帳號' }).click()
  await expect(page).toHaveURL(/\/app/)
}

export async function logout(page: Page) {
  await page.getByRole('button', { name: '登出' }).click()
  await expect(page).toHaveURL(/\/login/)
}

export async function createAccounts(page: Page, prefix: string, suffix: string): Promise<AccountPair> {
  const uniqueSuffix = `${suffix}-${Math.random().toString(36).slice(2, 8)}`
  const ownerUser = `${prefix}-owner-${uniqueSuffix}`
  const ownerPass = `Owner${suffix}A1`
  const otherUser = `${prefix}-peer-${uniqueSuffix}`
  const otherPass = `Peer${suffix}B1`

  await register(page, ownerUser, ownerPass)
  await logout(page)
  await register(page, otherUser, otherPass)
  await logout(page)

  return { ownerUser, ownerPass, otherUser, otherPass }
}

export async function createDocumentAndOpenDetail(page: Page, title: string, description = 'e2e') {
  await page.locator('a.nav-link[href="/app/files"]').click()
  await expect(page).toHaveURL(/\/app\/files/)
  await page.getByRole('button', { name: '新增文件' }).click()
  await page.getByRole('textbox', { name: '標題' }).fill(title)
  await page.getByRole('textbox', { name: '描述' }).fill(description)
  await page.getByRole('button', { name: '確認' }).click()

  const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: title }).first()
  await expect(row).toBeVisible()
  await row.locator('button').first().click()
  await expect(page).toHaveURL(/\/app\/documents\/\d+/)
  return Number(page.url().split('/').pop())
}

export async function createShareByUi(page: Page, targetUsername: string) {
  await page.getByRole('button', { name: '分享' }).click()
  const searchInput = page.getByPlaceholder('輸入帳號或 Email')
  await searchInput.fill(targetUsername)
  await page.getByRole('button', { name: '搜尋' }).click()

  const candidateRow = page.getByRole('row', { name: new RegExp(targetUsername) }).first()
  await expect(candidateRow).toBeVisible()
  await candidateRow.getByRole('button', { name: '新增分享' }).click()
  await expect(page.getByText('分享已新增')).toBeVisible()
  return searchInput
}

export async function closeShareDialog(page: Page) {
  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog')).toHaveCount(0)
}

export async function updateSharePermissionByApi(
  page: Page,
  documentId: number,
  username: string,
  permission: SharePermission,
) {
  return page.evaluate(
    async ({ apiBaseUrl, documentId, username, permission }) => {
      const token = window.localStorage.getItem('docflow.accessToken')
      if (!token) throw new Error('missing access token')

      const headers = {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
      }

      const shareResponse = await fetch(`${apiBaseUrl}/api/documents/${documentId}/shares`, { headers })
      if (!shareResponse.ok) throw new Error(`failed to load shares: ${shareResponse.status}`)

      const sharePayload = await shareResponse.json()
      const shares = sharePayload?.data ?? []
      const share = shares.find((item: { id: number; userId: number; username: string }) => item.username === username)
      if (!share) throw new Error('share target not found')

      const updateResponse = await fetch(`${apiBaseUrl}/api/documents/${documentId}/shares/${share.id}`, {
        method: 'PUT',
        headers,
        body: JSON.stringify({
          sharedWithUserId: share.userId,
          permission,
        }),
      })

      if (!updateResponse.ok) throw new Error(`failed to update share: ${updateResponse.status}`)
    },
    { apiBaseUrl, documentId, username, permission },
  )
}

export async function createShareByApiExpectStatus(
  page: Page,
  documentId: number,
  sharedWithUserId: number,
  permission: SharePermission,
) {
  return page.evaluate(
    async ({ apiBaseUrl, documentId, sharedWithUserId, permission }) => {
      const token = window.localStorage.getItem('docflow.accessToken')
      if (!token) throw new Error('missing access token')

      const response = await fetch(`${apiBaseUrl}/api/documents/${documentId}/shares`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          sharedWithUserId,
          permission,
        }),
      })

      return response.status
    },
    { apiBaseUrl, documentId, sharedWithUserId, permission },
  )
}

export async function assertActivityEntry(page: Page, pattern: RegExp) {
  await page.goto('/app')
  await expect(page.getByRole('heading', { name: '活動紀錄' })).toBeVisible()
  await expect(page.getByText(pattern)).toBeVisible()
}

export async function createFolder(page: Page, folderName: string) {
  await page.locator('a.nav-link[href="/app/files"]').click()
  await expect(page).toHaveURL(/\/app\/files/)
  await page.getByRole('button', { name: '新增資料夾' }).click()
  await page.getByRole('textbox', { name: '名稱' }).fill(folderName)
  await page.getByRole('button', { name: '儲存' }).click()
  await expect(page.locator('.node-name', { hasText: folderName }).first()).toBeVisible()
}
