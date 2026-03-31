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

  test('其他使用者看到 owner 的資料夾時，編輯與刪除按鈕為 disabled', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass, otherUser, otherPass } = await createAccounts(page, 'perm', suffix)
    const folderName = `E2E-資料夾權限-${suffix}`

    await login(page, ownerUser, ownerPass)
    await createFolder(page, folderName)
    await logout(page)

    await login(page, otherUser, otherPass)
    await page.getByRole('link', { name: '我的文件' }).click()

    const node = page.locator('.tree-node', {
      has: page.locator('.node-name', { hasText: folderName }),
    }).first()

    await expect(node).toBeVisible()
    await expect(node.getByRole('button', { name: '編輯' })).toBeDisabled()
    await expect(node.getByRole('button', { name: '刪除' })).toBeDisabled()
  })
})
