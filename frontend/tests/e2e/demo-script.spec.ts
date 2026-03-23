import path from 'node:path'
import { expect, test, type Page } from '@playwright/test'

const username = process.env.DOCFLOW_E2E_USERNAME
const password = process.env.DOCFLOW_E2E_PASSWORD

test.describe('DocFlow Lite Demo Script', () => {
  test.skip(!username || !password, '需提供 DOCFLOW_E2E_USERNAME 與 DOCFLOW_E2E_PASSWORD')

  async function login(page: Page) {
    await page.goto('/login')
    await page.getByRole('textbox', { name: 'Username' }).fill(username!)
    await page.getByRole('textbox', { name: 'Password' }).fill(password!)
    await page.getByRole('button', { name: '登入' }).click()
    await expect(page).toHaveURL(/\/app$/)
  }

  test('步驟 4 到步驟 8：folder、document、upload、detail、download', async ({ page }) => {
    const suffix = Date.now()
    const folderName = `Demo Folder ${suffix}`
    const documentTitle = `Demo Document ${suffix}`
    const uploadFilePath = path.resolve(__dirname, '../fixtures/demo-upload.txt')

    await login(page)
    await page.goto('/app/files')

    await test.step('4. 建立 folder', async () => {
      const folderCard = page.locator('.page-card').filter({
        has: page.getByRole('heading', { name: 'Folders' }),
      })
      await folderCard.getByRole('button', { name: '新增', exact: true }).click()

      const dialog = page.getByRole('dialog', { name: '新增資料夾' })
      await dialog.getByRole('textbox', { name: 'Name' }).fill(folderName)

      const createFolderResponse = page.waitForResponse((response) =>
        response.url().includes('/api/folders') && response.request().method() === 'POST',
      )
      await dialog.getByRole('button', { name: '儲存' }).click()

      const response = await createFolderResponse
      expect(response.ok(), `建立 folder 失敗，HTTP ${response.status()}`).toBeTruthy()

      await expect(dialog).toBeHidden()
      await expect(page.getByText(folderName)).toBeVisible()
      await page.getByText(folderName).click()
    })

    await test.step('5. 建立 document', async () => {
      await page.getByRole('button', { name: '新增文件' }).click()

      const dialog = page.getByRole('dialog', { name: '新增文件' })
      await dialog.getByRole('textbox', { name: 'Title' }).fill(documentTitle)
      await dialog.getByRole('textbox', { name: 'Description' }).fill('Created by Playwright demo script.')
      await dialog.getByRole('button', { name: '儲存' }).click()

      await expect(page.getByText(documentTitle)).toBeVisible()
    })

    await test.step('6. 上傳檔案', async () => {
      const row = page.locator('.el-table__body-wrapper tbody tr').filter({ hasText: documentTitle }).first()
      await row.locator('button').first().click()

      await expect(page).toHaveURL(/\/app\/documents\/\d+$/)
      await page.getByRole('button', { name: '上傳檔案' }).click()

      const uploadDialog = page.getByRole('dialog', { name: '上傳文件' })
      await uploadDialog.locator('input[type="file"]').setInputFiles(uploadFilePath)
      await uploadDialog.getByRole('button', { name: '上傳' }).click()

      await expect(page.getByText('demo-upload.txt')).toBeVisible()
    })

    await test.step('7. 確認 detail page', async () => {
      await expect(page.getByRole('heading', { name: 'Document Detail' })).toBeVisible()
      await expect(page.getByText(documentTitle)).toBeVisible()
    })

    await test.step('8. 下載檔案', async () => {
      const downloadPromise = page.waitForEvent('download')
      await page.getByRole('button', { name: '下載' }).click()
      const download = await downloadPromise

      expect(download.suggestedFilename()).toBe('demo-upload.txt')
    })
  })
})
