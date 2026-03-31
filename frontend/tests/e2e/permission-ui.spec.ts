import { expect, test } from '@playwright/test'
import {
  createShareByApiExpectStatus,
  closeShareDialog,
  createAccounts,
  createDocumentAndOpenDetail,
  createFolder,
  createShareByUi,
  login,
  logout,
} from './helpers/docflow'

test.describe('文件與資料夾權限 UI', () => {
  test('VIEW 分享文件在列表列顯示 disabled 的編輯、上傳、刪除', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass, otherUser, otherPass } = await createAccounts(page, 'perm', suffix)
    const title = `E2E-VIEW-列表-${suffix}`

    await login(page, ownerUser, ownerPass)
    await createDocumentAndOpenDetail(page, title, 'permission-e2e')
    await createShareByUi(page, otherUser)
    await closeShareDialog(page)
    await logout(page)

    await login(page, otherUser, otherPass)
    await page.getByRole('link', { name: '分享文件' }).click()

    const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: title }).first()
    await expect(row).toBeVisible()
    await expect(row.getByRole('button', { name: '編輯' })).toBeDisabled()
    await expect(row.getByRole('button', { name: '上傳' })).toBeDisabled()
    await expect(row.getByRole('button', { name: '刪除' })).toBeDisabled()
  })

  test('VIEW 分享文件在詳情頁顯示 disabled 的編輯、上傳檔案、刪除', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass, otherUser, otherPass } = await createAccounts(page, 'perm', suffix)
    const title = `E2E-VIEW-詳情-${suffix}`

    await login(page, ownerUser, ownerPass)
    await createDocumentAndOpenDetail(page, title, 'permission-e2e')
    await createShareByUi(page, otherUser)
    await closeShareDialog(page)
    await logout(page)

    await login(page, otherUser, otherPass)
    await page.getByRole('link', { name: '分享文件' }).click()

    const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: title }).first()
    await expect(row).toBeVisible()
    await row.locator('button').first().click()

    await expect(page.getByRole('button', { name: '編輯' })).toBeDisabled()
    await expect(page.getByRole('button', { name: '上傳檔案' })).toBeDisabled()
    await expect(page.getByRole('button', { name: '刪除' })).toBeDisabled()
  })

  test('被分享者在詳情頁看不到分享按鈕，且直打分享 API 會被拒絕', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass, otherUser, otherPass } = await createAccounts(page, 'perm', suffix)
    const title = `E2E-分享管理-${suffix}`

    await login(page, ownerUser, ownerPass)
    const documentId = await createDocumentAndOpenDetail(page, title, 'permission-e2e')
    await createShareByUi(page, otherUser)
    await closeShareDialog(page)
    await logout(page)

    await login(page, otherUser, otherPass)
    await page.getByRole('link', { name: '分享文件' }).click()

    const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: title }).first()
    await expect(row).toBeVisible()
    await row.locator('button').first().click()

    await expect(page.getByRole('button', { name: '分享' })).toHaveCount(0)

    const forbiddenStatus = await createShareByApiExpectStatus(page, documentId, 999999, 'VIEW')
    await expect(forbiddenStatus).toBe(403)
  })

  test('其他使用者看不到 owner 的資料夾', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass, otherUser, otherPass } = await createAccounts(page, 'perm', suffix)
    const folderName = `E2E-私有資料夾-${suffix}`

    await login(page, ownerUser, ownerPass)
    await createFolder(page, folderName)
    await logout(page)

    await login(page, otherUser, otherPass)
    await page.getByRole('link', { name: '我 我的文件' }).click()

    await expect(page.getByRole('heading', { name: '自己的資料夾' })).toBeVisible()
    await expect(page.getByText('目前沒有自己的資料夾')).toBeVisible()
    await expect(page.locator('.node-name', { hasText: folderName })).toHaveCount(0)
    await expect(page.getByText(folderName)).toHaveCount(0)
  })

  test('從非文件頁登出後，下一位使用者不會沿用上一位的資料夾選取', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass, otherUser, otherPass } = await createAccounts(page, 'perm', suffix)
    const folderName = `E2E-跨帳號選取-${suffix}`

    await login(page, ownerUser, ownerPass)
    await createFolder(page, folderName)
    await page.locator('.node-name', { hasText: folderName }).first().click()
    await expect(page.getByText(`目前顯示自己的資料夾「${folderName}」的文件`)).toBeVisible()
    await page.getByRole('link', { name: '分享文件' }).click()
    await logout(page)

    await login(page, otherUser, otherPass)
    await page.getByRole('link', { name: '我 我的文件' }).click()

    await expect(page.getByText(`目前顯示自己的資料夾「${folderName}」的文件`)).toHaveCount(0)
    await expect(page.getByText('顯示自己的文件')).toBeVisible()
    await expect(page.getByText(/資料夾 #/)).toHaveCount(0)
    await expect(page.locator('.node-name', { hasText: folderName })).toHaveCount(0)
  })

  test('零資料夾時仍可建立第一個根資料夾，且重新進入我的文件不會過早清除有效選取', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass } = await createAccounts(page, 'perm', suffix)
    const folderName = `E2E-根資料夾-${suffix}`
    const documentTitle = `E2E-自己的文件-${suffix}`

    await login(page, ownerUser, ownerPass)
    await createFolder(page, folderName)
    await page.locator('.node-name', { hasText: folderName }).first().click()
    await expect(page.getByText(`目前顯示自己的資料夾「${folderName}」的文件`)).toBeVisible()
    await page.getByRole('link', { name: '分享文件' }).click()
    await page.locator('a.nav-link[href="/app/files"]').click()
    await expect(page.getByText(`目前顯示自己的資料夾「${folderName}」的文件`)).toBeVisible()
    await expect(page.getByText('顯示自己的文件')).toHaveCount(0)

    await page.getByRole('button', { name: '回到全部文件' }).click()
    await expect(page.getByText('顯示自己的文件')).toBeVisible()

    await createDocumentAndOpenDetail(page, documentTitle, 'permission-e2e')
    await page.locator('a.nav-link[href="/app/files"]').click()

    const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: documentTitle }).first()
    await expect(row).toBeVisible()
  })
})
