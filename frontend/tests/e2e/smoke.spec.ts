import { expect, test, type Page } from '@playwright/test'

const username = process.env.DOCFLOW_E2E_USERNAME
const password = process.env.DOCFLOW_E2E_PASSWORD

test.describe('DocFlow Lite 部署站 smoke test', () => {
  test.skip(!username || !password, '需提供 DOCFLOW_E2E_USERNAME 與 DOCFLOW_E2E_PASSWORD')

  async function login(page: Page) {
    await page.goto('/login')

    await expect(page.getByRole('heading', { name: 'DocFlow Lite' })).toBeVisible()
    await page.getByRole('textbox', { name: 'Username' }).fill(username!)
    await page.getByRole('textbox', { name: 'Password' }).fill(password!)
    await page.getByRole('button', { name: '登入' }).click()

    await expect(page).toHaveURL(/\/app$/)
    await expect(page.getByText(username!)).toBeVisible()
  }

  test('未登入使用者會被導回登入頁', async ({ page }) => {
    await page.goto('/app')

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('button', { name: '登入' })).toBeVisible()
  })

  test('可以登入並登出', async ({ page }) => {
    await login(page)

    await page.getByRole('button', { name: '登出' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByRole('button', { name: '登入' })).toBeVisible()
  })

  test('Dashboard 主要區塊應成功載入', async ({ page }) => {
    await login(page)

    await expect(page.getByRole('heading', { name: 'Folders' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Documents' })).toBeVisible()
    await expect(page.getByText('Folder tree 載入失敗')).toHaveCount(0)
    await expect(page.getByText('Document list 載入失敗')).toHaveCount(0)
  })

  test('Insights 主要區塊應成功載入', async ({ page }) => {
    await login(page)

    await page.getByRole('link', { name: 'Insights' }).click()

    await expect(page).toHaveURL(/\/app\/insights$/)
    await expect(page.getByRole('heading', { name: 'Hot Documents' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Recent Views' })).toBeVisible()
    await expect(page.getByRole('heading', { name: 'Activity Logs' })).toBeVisible()
    await expect(page.getByText('熱門文件載入失敗')).toHaveCount(0)
    await expect(page.getByText('最近查看資料載入失敗')).toHaveCount(0)
    await expect(page.getByText('活動紀錄載入失敗')).toHaveCount(0)
  })
})
