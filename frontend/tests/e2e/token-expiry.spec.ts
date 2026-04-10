import { expect, test, type Page, type Route } from '@playwright/test'

const SESSION_EXPIRED_MESSAGE = '?»å…¥?€?‹å·²?Žæ?ï¼Œè??æ–°?»å…¥??
const TEST_USER = {
  id: 1,
  username: 'admin',
  email: 'admin@example.com',
  role: 'ADMIN',
  status: 'ACTIVE',
  mustChangePassword: false,
}

function jsonResponse(data: unknown, status = 200) {
  return {
    status,
    contentType: 'application/json',
    body: JSON.stringify({
      success: status >= 200 && status < 300,
      data,
      message: null,
    }),
  }
}

function fulfillUnauthorized(route: Route) {
  return route.fulfill(jsonResponse(null, 401))
}

async function fulfillOk(route: Route, data: unknown) {
  await route.fulfill(jsonResponse(data))
}

async function seedAuthenticatedSession(
  page: Page,
  tokens: { accessToken: string; refreshToken: string },
) {
  await page.addInitScript(
    ({ user, accessToken, refreshToken }) => {
      window.localStorage.setItem('docflow.accessToken', accessToken)
      window.localStorage.setItem('docflow.refreshToken', refreshToken)
      window.localStorage.setItem('docflow.user', JSON.stringify(user))
    },
    {
      user: TEST_USER,
      accessToken: tokens.accessToken,
      refreshToken: tokens.refreshToken,
    },
  )
}

async function mockCommonAppApis(page: Page) {
  await page.route('**/api/documents**', async (route) => {
    await fulfillOk(route, {
      items: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })
  })
  await page.route('**/api/folders/tree', async (route) => {
    await fulfillOk(route, [])
  })
  await page.route('**/api/stats/hot-documents', async (route) => {
    await fulfillOk(route, [])
  })
  await page.route('**/api/users/me/recent-views', async (route) => {
    await fulfillOk(route, [])
  })
  await page.route('**/api/activities**', async (route) => {
    await fulfillOk(route, {
      items: [],
      page: 0,
      size: 10,
      totalElements: 0,
      totalPages: 0,
    })
  })
  await page.route('**/api/auth/logout', async (route) => {
    await route.fulfill({
      status: 200,
      body: '',
    })
  })
}

async function mockLoginSuccess(page: Page) {
  await page.route('**/api/auth/login', async (route) => {
    const payload = route.request().postDataJSON() as { username: string; password: string }
    expect(payload).toMatchObject({
      username: 'admin',
      password: 'admin123',
    })

    await fulfillOk(route, {
      user: TEST_USER,
      tokens: {
        accessToken: 'fresh-access-token',
        refreshToken: 'fresh-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
      },
    })
  })
}

test.describe('token expiry flow', () => {
  test('refresh ?å?å¾Œæ??™åœ¨?‰ç”¨?é¢ä¸¦æ›´??access token', async ({ page }) => {
    let meRequestCount = 0
    let refreshRequestCount = 0

    await mockCommonAppApis(page)
    await seedAuthenticatedSession(page, {
      accessToken: 'expired-access-token',
      refreshToken: 'valid-refresh-token',
    })

    await page.route('**/api/auth/refresh', async (route) => {
      refreshRequestCount += 1
      await fulfillOk(route, {
        accessToken: 'refreshed-access-token',
        refreshToken: 'rotated-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 3600,
      })
    })

    await page.route('**/api/auth/me', async (route) => {
      meRequestCount += 1
      const authorization = route.request().headers().authorization

      if (meRequestCount === 1) {
        expect(authorization).toBe('Bearer expired-access-token')
        await fulfillUnauthorized(route)
        return
      }

      expect(authorization).toBe('Bearer refreshed-access-token')
      await fulfillOk(route, TEST_USER)
    })

    await page.goto('/app')

    await expect(page).toHaveURL(/\/app$/)
    await expect.poll(() => meRequestCount).toBe(2)
    expect(refreshRequestCount).toBe(1)

    const accessToken = await page.evaluate(() => window.localStorage.getItem('docflow.accessToken'))
    const refreshToken = await page.evaluate(() => window.localStorage.getItem('docflow.refreshToken'))

    expect(accessToken).toBe('refreshed-access-token')
    expect(refreshToken).toBe('rotated-refresh-token')
  })

  test('refresh å¤±æ??‚æ?å°Žå??»å…¥?ä¸¦é¡¯ç¤º?Žæ?è¨Šæ¯', async ({ page }) => {
    await mockCommonAppApis(page)
    await seedAuthenticatedSession(page, {
      accessToken: 'expired-access-token',
      refreshToken: 'invalid-refresh-token',
    })

    await page.route('**/api/auth/me', fulfillUnauthorized)
    await page.route('**/api/auth/refresh', fulfillUnauthorized)

    await page.goto('/app')

    await expect(page).toHaveURL(/\/login\?redirect=.+$/)
    await expect
      .poll(() => page.evaluate(() => window.location.pathname))
      .toBe('/login')
    await expect
      .poll(() => page.evaluate(() => new URLSearchParams(window.location.search).get('redirect')))
      .toBe('/app')
    await expect(page.getByRole('alert')).toContainText(SESSION_EXPIRED_MESSAGE)

    await expect
      .poll(async () => {
        try {
          return await page.evaluate(() => ({
            accessToken: window.localStorage.getItem('docflow.accessToken'),
            refreshToken: window.localStorage.getItem('docflow.refreshToken'),
            user: window.localStorage.getItem('docflow.user'),
          }))
        } catch {
          return null
        }
      })
      .toEqual({
        accessToken: null,
        refreshToken: null,
        user: null,
      })
  })

  test('?—ä?è­·é??¢å??žç™»?¥å??æ–°?»å…¥?ƒå??°å??®æ???, async ({ page }) => {
    await mockCommonAppApis(page)
    await mockLoginSuccess(page)

    await page.goto('/app/files')

    await expect(page).toHaveURL(/\/login\?redirect=.+$/)
    await expect
      .poll(() => page.evaluate(() => new URLSearchParams(window.location.search).get('redirect')))
      .toBe('/app/files')

    await page.getByRole('textbox', { name: 'ä½¿ç”¨?…å?ç¨? }).fill('admin')
    await page.getByLabel('å¯†ç¢¼').fill('admin123')
    await page.getByRole('button', { name: '?»å…¥' }).click()

    await expect(page).toHaveURL(/\/app\/files$/)
  })

  test('?‹å??»å‡ºå¾Œå??°ç™»?¥é?ä½†ä?é¡¯ç¤º?Žæ?è¨Šæ¯', async ({ page }) => {
    await mockCommonAppApis(page)
    await seedAuthenticatedSession(page, {
      accessToken: 'valid-access-token',
      refreshToken: 'valid-refresh-token',
    })

    await page.route('**/api/auth/me', async (route) => {
      await fulfillOk(route, TEST_USER)
    })

    await page.goto('/app')
    await expect(page).toHaveURL(/\/app$/)

    await page.getByRole('button', { name: '?»å‡º' }).click()

    await expect(page).toHaveURL(/\/login$/)
    await expect(page.getByText(SESSION_EXPIRED_MESSAGE)).toHaveCount(0)
  })
})
