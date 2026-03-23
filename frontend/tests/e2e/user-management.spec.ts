import { expect, test } from '@playwright/test'

test('登入頁應顯示註冊入口', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByRole('link', { name: '註冊' })).toBeVisible()
})
