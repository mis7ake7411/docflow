import { expect, test, type Page } from '@playwright/test'

const ownerUser = process.env.DOCFLOW_E2E_OWNER_USERNAME
const ownerPass = process.env.DOCFLOW_E2E_OWNER_PASSWORD
const otherUser = process.env.DOCFLOW_E2E_OTHER_USERNAME
const otherPass = process.env.DOCFLOW_E2E_OTHER_PASSWORD

test.describe('文件/資料夾權限 UI', () => {
  test.skip(!ownerUser || !ownerPass || !otherUser || !otherPass, '需提供兩組帳號')

  async function login(page: Page, username: string, password: string) {
    await page.goto('/login')
    await page.getByRole('textbox', { name: '帳號' }).fill(username)
    await page.getByRole('textbox', { name: '密碼' }).fill(password)
    await page.getByRole('button', { name: '登入' }).click()
    await expect(page).toHaveURL(/\/app/)
  }

  test('他人文件按鈕為 disabled 並顯示提示', async ({ page }) => {
    const title = `E2E-他人文件-${Date.now()}`

    await login(page, ownerUser!, ownerPass!)
    await page.getByRole('link', { name: '文件管理' }).click()
    await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible()

    await page.getByRole('button', { name: '新增文件' }).click()
    await page.getByRole('textbox', { name: '標題' }).fill(title)
    await page.getByRole('textbox', { name: '描述' }).fill('e2e')
    await page.getByRole('button', { name: '確認' }).click()
    await expect(page.getByText('文件已建立')).toBeVisible()
    await expect(page.getByRole('cell', { name: title })).toBeVisible()

    await page.getByRole('button', { name: '登出' }).click()
    await expect(page).toHaveURL(/\/login/)

    await login(page, otherUser!, otherPass!)
    await page.getByRole('link', { name: '文件管理' }).click()

    await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible()

    const row = page.getByRole('row', { name: new RegExp(title) })
    const editButton = row.getByRole('button', { name: '編輯' })
    const uploadButton = row.getByRole('button', { name: '上傳' })
    const deleteButton = row.getByRole('button', { name: '刪除' })

    await expect(editButton).toBeDisabled()
    await expect(uploadButton).toBeDisabled()
    await expect(deleteButton).toBeDisabled()

    await editButton.hover()
    await expect(page.getByText('僅能修改自己建立的文件')).toBeVisible()
  })

  test('他人文件詳情按鈕 disabled 並顯示提示', async ({ page }) => {
    const title = `E2E-他人詳情-${Date.now()}`

    await login(page, ownerUser!, ownerPass!)
    await page.getByRole('link', { name: '文件管理' }).click()
    await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible()

    await page.getByRole('button', { name: '新增文件' }).click()
    await page.getByRole('textbox', { name: '標題' }).fill(title)
    await page.getByRole('textbox', { name: '描述' }).fill('e2e')
    await page.getByRole('button', { name: '確認' }).click()
    await expect(page.getByText('文件已建立')).toBeVisible()

    await page.getByRole('button', { name: '登出' }).click()
    await expect(page).toHaveURL(/\/login/)

    await login(page, otherUser!, otherPass!)
    await page.getByRole('link', { name: '文件管理' }).click()
    await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible()

    await page.getByRole('row', { name: new RegExp(title) }).getByRole('button', { name: '查看' }).click()
    await expect(page.getByRole('heading', { name: '文件明細' })).toBeVisible()

    const editButton = page.getByRole('button', { name: '編輯' })
    const uploadButton = page.getByRole('button', { name: '上傳檔案' })
    const deleteButton = page.getByRole('button', { name: '刪除' })

    await expect(editButton).toBeDisabled()
    await expect(uploadButton).toBeDisabled()
    await expect(deleteButton).toBeDisabled()

    await editButton.hover()
    await expect(page.getByText('僅能修改自己建立的文件', { exact: true })).toBeVisible()
    await expect(page.getByText('你僅能修改自己建立的文件')).toBeVisible()
  })

  test('他人資料夾按鈕 disabled 並顯示提示', async ({ page }) => {
    const folderName = `E2E-他人資料夾-${Date.now()}`

    await login(page, ownerUser!, ownerPass!)
    await page.getByRole('link', { name: '文件管理' }).click()
    await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible()

    await page.getByRole('button', { name: '新增資料夾' }).click()
    await page.getByRole('textbox', { name: '名稱' }).fill(folderName)
    await page.getByRole('button', { name: '儲存' }).click()
    await expect(page.getByText('資料夾建立成功')).toBeVisible()
    await expect(page.locator('.node-name', { hasText: folderName }).first()).toBeVisible()

    await page.getByRole('button', { name: '登出' }).click()
    await expect(page).toHaveURL(/\/login/)

    await login(page, otherUser!, otherPass!)
    await page.getByRole('link', { name: '文件管理' }).click()
    await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible()

    const node = page.locator('.tree-node', {
      has: page.locator('.node-name', { hasText: folderName }),
    }).first()
    const editButton = node.getByRole('button', { name: '編輯' })
    const deleteButton = node.getByRole('button', { name: '刪除' })

    await expect(editButton).toBeDisabled()
    await expect(deleteButton).toBeDisabled()

    await editButton.hover({ force: true })
    await expect(page.getByText('僅能修改自己建立的資料夾', { exact: true })).toBeVisible()
  })
})
