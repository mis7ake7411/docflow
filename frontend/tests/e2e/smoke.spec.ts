import { expect, test, type Page } from '@playwright/test'

const username = process.env.DOCFLOW_E2E_USERNAME
const password = process.env.DOCFLOW_E2E_PASSWORD

test.describe('DocFlow Lite 佈署 smoke test', () => {
  test.skip(!username || !password, '需先設定 DOCFLOW_E2E_USERNAME 與 DOCFLOW_E2E_PASSWORD')

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

  test('文件管理主要區塊應成功載入', async ({ page }) => {
    await login(page)

    await page.getByRole('link', { name: '文件管理' }).click()

    await expect(page).toHaveURL(/\/app\/files$/)
    await expect(page.getByRole('heading', { name: '資料夾列表' })).toBeVisible()
    await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible()
    await expect(page.getByText('資料夾樹載入失敗')).toHaveCount(0)
    await expect(page.getByText('文件清單載入失敗')).toHaveCount(0)
  })

  test('洞察報表主要區塊應成功載入', async ({ page }) => {
    await login(page)

    await page.getByRole('link', { name: '洞察報表' }).click()

    await expect(page).toHaveURL(/\/app\/insights$/)
    await expect(page.getByRole('heading', { name: '熱門文件' })).toBeVisible()
    await expect(page.getByRole('heading', { name: '最近瀏覽' })).toBeVisible()
    await expect(page.getByRole('heading', { name: '活動紀錄' })).toBeVisible()
    await expect(page.getByText('熱門文件載入失敗')).toHaveCount(0)
    await expect(page.getByText('最近瀏覽載入失敗')).toHaveCount(0)
    await expect(page.getByText('活動紀錄載入失敗')).toHaveCount(0)
  })
})
