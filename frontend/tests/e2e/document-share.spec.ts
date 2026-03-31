import { expect, test } from '@playwright/test'
import {
  assertActivityEntry,
  createAccounts,
  createDocumentAndOpenDetail,
  createShareByUi,
  login,
  logout,
  updateSharePermissionByApi,
} from './helpers/docflow'

test.describe('文件分享', () => {
  test('owner 可分享文件並在儀表板看到新增分享紀錄', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass, otherUser } = await createAccounts(page, 'share', suffix)
    const title = `E2E-分享建立-${suffix}`

    await login(page, ownerUser, ownerPass)
    await createDocumentAndOpenDetail(page, title, 'share-e2e')
    await createShareByUi(page, otherUser)

    const shareRow = page.getByRole('row', { name: new RegExp(otherUser) }).last()
    await expect(shareRow).toContainText(/VIEW|可檢視/)
    await assertActivityEntry(page, new RegExp(`${otherUser}.*可檢視`))
  })

  test('分享權限升級為可編輯後，被分享者可操作文件並看到更新紀錄', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass, otherUser, otherPass } = await createAccounts(page, 'share', suffix)
    const title = `E2E-分享升級-${suffix}`

    await login(page, ownerUser, ownerPass)
    const documentId = await createDocumentAndOpenDetail(page, title, 'share-e2e')
    await createShareByUi(page, otherUser)
    await updateSharePermissionByApi(page, documentId, otherUser, 'EDIT')
    await assertActivityEntry(page, new RegExp(`${otherUser}.*可檢視.*可編輯`))

    await logout(page)

    await login(page, otherUser, otherPass)
    await page.getByRole('link', { name: '分享文件' }).click()
    const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: title }).first()
    await expect(row).toBeVisible()
    await row.locator('button').first().click()
    await expect(page.getByRole('button', { name: '編輯' })).toBeEnabled()
    await expect(page.getByRole('button', { name: '上傳檔案' })).toBeEnabled()
  })

  test('owner 取消分享後，被分享者不可再看到文件，且儀表板有取消紀錄', async ({ page }) => {
    const suffix = String(Date.now())
    const { ownerUser, ownerPass, otherUser, otherPass } = await createAccounts(page, 'share', suffix)
    const title = `E2E-分享移除-${suffix}`

    await login(page, ownerUser, ownerPass)
    await createDocumentAndOpenDetail(page, title, 'share-e2e')
    await createShareByUi(page, otherUser)

    const removableShareRow = page.getByRole('row', { name: new RegExp(otherUser) }).last()
    await removableShareRow.getByRole('button', { name: '移除' }).click()
    await page.getByRole('button', { name: '確認' }).click()
    await expect(page.getByText('分享已移除')).toBeVisible()
    await assertActivityEntry(page, new RegExp(`已取消\\s*${otherUser}`))

    await logout(page)

    await login(page, otherUser, otherPass)
    await page.getByRole('link', { name: '分享文件' }).click()
    await expect(page.getByRole('cell', { name: title })).toHaveCount(0)
  })
})
