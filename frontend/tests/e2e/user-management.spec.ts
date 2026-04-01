import { expect, test } from '@playwright/test'

test('登入頁應顯示註冊入口', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('link', { name: '註冊' })).toBeVisible()
})

test('登入頁不應顯示使用者管理導覽', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByText('使用者管理')).toHaveCount(0)
})

