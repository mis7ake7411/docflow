import { expect, test, type Page } from '@playwright/test'

const ownerUser = process.env.DOCFLOW_E2E_OWNER_USERNAME
const ownerPass = process.env.DOCFLOW_E2E_OWNER_PASSWORD
const otherUser = process.env.DOCFLOW_E2E_OTHER_USERNAME
const otherPass = process.env.DOCFLOW_E2E_OTHER_PASSWORD

test.describe('文件/資料夾權限 UI', () => {
  test.skip(!ownerUser || !ownerPass || !otherUser || !otherPass, '需提供兩組帳號')

  async function login(page: Page, username: string, password: string) {
    await page.goto('/login')
    await page.getByRole('textbox', { name: 'Username' }).fill(username)
    await page.getByRole('textbox', { name: 'Password' }).fill(password)
    await page.getByRole('button', { name: '登入' }).click()
    await expect(page).toHaveURL(/\/app/)
  }

  test('他人文件按鈕為 disabled 並顯示提示', async ({ page }) => {
    await login(page, otherUser!, otherPass!)
    await page.getByRole('link', { name: '文件管理' }).click()

    await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible()

    // TODO: 依實作選擇一筆非本人文件列，確認「編輯/上傳/刪除」為 disabled 並有提示；目前仍待補上選取非擁有者文件列的步驟
  })
})
