# 登入功能優化 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 讓登入流程在 session 過期、token 刷新、重新登入回跳與手動登出情境下具備一致體驗，並在第二階段補強 refresh token 安全模型。

**Architecture:** 以既有 Spring Boot 認證模組與 Vue 前端 auth store 為基礎，第一階段先收斂前端 session 狀態機、401 刷新流程與驗收測試，不變更核心資料模型；第二階段再升級 refresh token 為可輪替、可撤銷、可追蹤的 session 模型。整體採 MVP 切片，先交付可見穩定性，再處理安全性深度強化。

**Tech Stack:** Spring Boot, Spring Security, Spring Data JPA, Vue 3, Pinia, Axios, Vitest, Playwright, JUnit 5

---

## Constitution Check（前）

- 規格是否避免實作細節：通過。規格以使用者價值、驗收情境與成功準則為主，未把框架或資料庫實作帶入需求層。
- 是否以 MVP 為最小切片：通過。計畫拆為第一期穩定性、第二期安全性，避免一次改動過大。
- 使用者故事是否可獨立驗收：通過。第一期可單獨驗證登入失效恢復與回跳；第二期可單獨驗證 token rotation 與 session 撤銷。
- 成功準則是否可量測且技術無關：通過。均以可觀察行為與可驗證結果表述。
- 釐清事項是否 <= 3：通過。本計畫直接採用已確認的兩階段方案，無新增待定需求。
- 是否拒絕過度設計：通過。暫不納入第三方登入、MFA、裝置管理 UI。

## 檔案結構與責任

- Modify: `frontend/src/stores/auth.ts`
  - 統一登入、session 過期、手動登出、bootstrap 邏輯，拆開「清空 session」與「保留失效原因」責任。
- Modify: `frontend/src/shared/api/axios.ts`
  - 實作 401 單次共享 refresh 流程、統一 refresh 失敗後的登出與回跳策略。
- Modify: `frontend/src/features/auth/api.ts`
  - 對齊 refresh API 回傳的新 token 組與前端呼叫契約。
- Modify: `frontend/src/features/auth/components/AuthBootstrap.vue`
  - 讓初始化流程與 bridge 事件對齊新的 auth store 行為。
- Modify: `frontend/src/pages/LoginPage.vue`
  - 顯示 session 過期提示並確認登入後提示清除。
- Modify: `frontend/src/features/auth/components/LoginForm.vue`
  - 維持 redirect 回跳與登入成功後狀態清理。
- Test: `frontend/tests/unit/auth-store.spec.ts`
  - 補 auth store session 狀態轉移測試。
- Test: `frontend/tests/unit/api-client.spec.ts`
  - 新增 axios 401/refresh 併發控制測試。
- Test: `frontend/tests/unit/auth-refresh-rotation.spec.ts`
  - 驗證 rotation 後新的 refresh token 會被持久化，且下一次 refresh 或重新載入仍可使用。
- Test: `frontend/tests/e2e/token-expiry.spec.ts`
  - 驗證 token 過期、自動刷新、失敗導回登入與回跳。
- Modify: `src/main/java/com/docflow/auth/entity/RefreshToken.java`
  - 第二期擴充 session 欄位與儲存責任。
- Modify: `src/main/java/com/docflow/auth/repository/RefreshTokenRepository.java`
  - 支援以 token 雜湊值、session 識別值與撤銷狀態查詢。
- Modify: `src/main/java/com/docflow/auth/service/AuthServiceImpl.java`
  - 第二期實作 refresh token rotation、目前 session 撤銷與雙 session 驗證情境。
- Modify: `src/main/java/com/docflow/auth/controller/AuthController.java`
  - 對齊 refresh 回傳格式與目前 session 撤銷行為。
- Modify: `src/main/java/com/docflow/common/exception/GlobalExceptionHandler.java`
  - 統一回傳可供前端判斷的失敗訊息。
- Test: `src/test/java/com/docflow/auth/AuthServiceImplTest.java`
  - 補 refresh rotation、舊 token 失效、雙 session 與目前 session 撤銷驗證。
- Test: `src/test/java/com/docflow/auth/AuthControllerTest.java`
  - 驗證 refresh、logout 與雙 session API 行為。
- Test: `src/test/java/com/docflow/auth/RefreshTokenRepositoryTest.java`
  - 驗證 schema / mapping / repository 查詢在測試環境可用。

## 複雜度追蹤表

| 複雜度來源 | 原因 | 決策 | 拒絕的替代方案與理由 |
| --- | --- | --- | --- |
| 單次共享 refresh 流程 | 多個 API 同時 401 時會競爭刷新 | 保留（第一期核心需求） | 維持每個請求各自 refresh，會造成競爭與不一致 |
| redirect 回跳 | 需保留原目標頁並在登入後回復 | 保留（直接影響使用流程） | 一律導回首頁會破壞使用者連續操作 |
| refresh token rotation | 牽動 token 換發與儲存策略 | 保留（第二期核心安全需求） | 維持可重放 token 風險過高 |
| 多 session 支援 | 需有 session 撤銷與追蹤欄位 | 保留（第二期安全模型必要） | 僅保留單一全域 session，無法驗證目前 session 撤銷不影響其他登入 |
| 裝置管理 UI | 需額外前端介面與管理流程 | 拒絕（超出 MVP） | 延後到有明確需求再規劃 |

## 非目標與風險控制

- 本計畫不處理第三方登入、MFA、密碼重設郵件流程。
- 第一階段不新增後端資料表或新 API，避免把穩定性修補與安全模型改造綁在一起。
- 第二階段不新增 session 管理 UI；後端只處理目前 session 的 rotation / revoke 與雙 session 驗證。
- 第二階段沿用現有 `ddl-auto: update` / 測試環境 `create-drop` 假設，不額外引入 migration 工具；但必須以 repository 測試驗證 schema 與查詢可正常運作。
- 若現有測試檔已有編碼亂碼，優先以本次功能覆蓋的斷言為主，不順手擴大清理其他非必要文字問題。

---

### Task 1: 第一期收斂 auth store 的 session 狀態機

**Files:**
- Modify: `frontend/src/stores/auth.ts`
- Test: `frontend/tests/unit/auth-store.spec.ts`

- [ ] **Step 1: 先改寫 auth store 單元測試，明確描述第一期目標行為**

新增或重寫以下測試情境：

```ts
it('session 過期時應保留原因並清空 token 與 user')
it('手動登出時不應留下 session 過期原因')
it('登入成功時應清除舊的 session 過期原因')
it('bootstrap 失敗時應清空登入狀態')
```

- [ ] **Step 2: 執行 auth store 單元測試，確認目前至少有一項失敗**

Run: `npm exec --prefix frontend vitest run tests/unit/auth-store.spec.ts`
Expected: FAIL，顯示 `sessionExpiredReason` 被清空或登入後未正確重置。

- [ ] **Step 3: 在 `auth.ts` 拆分 `clearAuth()` 與 `setSessionExpired()` 的責任**

最小實作方向：

```ts
clearAuth(options?: { clearReason?: boolean }) {
  this.accessToken = null
  this.refreshToken = null
  this.user = null
  if (options?.clearReason ?? true) {
    this.sessionExpiredReason = null
  }
}

setSessionExpired(reason = '您的登入已過期，請重新登入') {
  this.sessionExpiredReason = reason
  this.clearAuth({ clearReason: false })
}
```

- [ ] **Step 4: 調整 `login()`、`register()`、`logout()`、`bootstrapAuth()`，讓成功與失敗路徑各自清楚**

目標：
- 登入成功會清除過期原因
- 手動登出不保留過期原因
- bootstrap 失敗視情境清空狀態

- [ ] **Step 5: 重跑 auth store 單元測試，確認全部通過**

Run: `npm exec --prefix frontend vitest run tests/unit/auth-store.spec.ts`
Expected: PASS

- [ ] **Step 6: Commit 第一階段 auth store 狀態機調整**

```bash
git add frontend/src/stores/auth.ts frontend/tests/unit/auth-store.spec.ts
git commit -m "fix: stabilize auth store session state"
```

### Task 2: 第一期收斂 axios 的 401 刷新流程

**Files:**
- Modify: `frontend/src/shared/api/axios.ts`
- Test: `frontend/tests/unit/api-client.spec.ts`

- [ ] **Step 1: 新增 axios 攔截器測試，驗證多個 401 共用一次 refresh**

新增測試情境：

```ts
it('多個 401 請求應共用同一次 refresh')
it('refresh 成功後應重送原請求')
it('refresh 失敗後應只觸發一次 logout 與導頁')
```

- [ ] **Step 2: 執行新測試，確認目前失敗**

Run: `npm exec --prefix frontend vitest run tests/unit/api-client.spec.ts`
Expected: FAIL，因目前攔截器未做 refresh promise 共用。

- [ ] **Step 3: 在 `axios.ts` 加入模組層級的 refresh promise 共用機制**

最小實作方向：

```ts
let refreshInFlight: Promise<string | null> | null = null

async function getRefreshedAccessToken() {
  if (!refreshInFlight) {
    refreshInFlight = refreshAccessTokenFromBridge().finally(() => {
      refreshInFlight = null
    })
  }
  return refreshInFlight
}
```

- [ ] **Step 4: 將 401 分支改為等待共用 refresh，失敗時只走一次統一路徑**

目標：
- 原請求只重送一次
- `/login`、`/register` 不重複導頁
- refresh 失敗時保留 redirect

- [ ] **Step 5: 重跑 API client 單元測試**

Run: `npm exec --prefix frontend vitest run tests/unit/api-client.spec.ts`
Expected: PASS

- [ ] **Step 6: Commit 401 刷新流程調整**

```bash
git add frontend/src/shared/api/axios.ts frontend/tests/unit/api-client.spec.ts
git commit -m "fix: serialize token refresh flow"
```

### Task 3: 第一期對齊登入頁、bootstrap 與回跳流程

**Files:**
- Modify: `frontend/src/features/auth/components/AuthBootstrap.vue`
- Modify: `frontend/src/pages/LoginPage.vue`
- Modify: `frontend/src/features/auth/components/LoginForm.vue`
- Test: `frontend/tests/e2e/token-expiry.spec.ts`

- [ ] **Step 1: 先更新 E2E 驗收情境，使其對齊第一期成功準則**

至少保留並修正以下情境：
- token 過期後 refresh 成功，仍留在原頁
- refresh 失敗後導回登入頁並顯示提示
- 帶 `redirect` 參數重新登入後回原頁
- 手動登出後回登入頁且不顯示過期提示

- [ ] **Step 2: 執行 token expiry E2E，確認目前至少有一項失敗**

Run: `npm run test:e2e -- --project=chromium --grep "Token"`
Expected: FAIL，可能出現在提示訊息、回跳或多次刷新行為。

- [ ] **Step 3: 調整 `AuthBootstrap.vue`，讓初始化與 bridge logout 對齊新的 store 行為**

重點：
- `onLogout(reason)` 只負責標記 session 失效與清空狀態
- 初始 bootstrap 若未登入，只導到登入頁，不誤設過期提示

- [ ] **Step 4: 調整 `LoginPage.vue` 與 `LoginForm.vue`，確保登入成功後提示清除且 redirect 生效**

重點：
- 只顯示當前有效的過期提示
- 成功登入後清除提示並導回原頁

- [ ] **Step 5: 重跑前端 build 與 token expiry E2E**

Run: `npm run build`
Run: `npm run test:e2e -- --project=chromium --grep "Token"`
Expected: PASS

- [ ] **Step 6: Commit 第一階段登入頁與 bootstrap 收斂**

```bash
git add frontend/src/features/auth/components/AuthBootstrap.vue frontend/src/pages/LoginPage.vue frontend/src/features/auth/components/LoginForm.vue frontend/tests/e2e/token-expiry.spec.ts
git commit -m "fix: align login bootstrap and redirect flow"
```

### Task 4: 第二期擴充 refresh token session 模型與 schema 驗證

**Files:**
- Modify: `src/main/java/com/docflow/auth/entity/RefreshToken.java`
- Modify: `src/main/java/com/docflow/auth/repository/RefreshTokenRepository.java`
- Modify: `src/main/java/com/docflow/auth/service/AuthServiceImpl.java`
- Test: `src/test/java/com/docflow/auth/AuthServiceImplTest.java`
- Test: `src/test/java/com/docflow/auth/RefreshTokenRepositoryTest.java`

- [ ] **Step 1: 先新增後端測試，描述 refresh token rotation 的期望**

新增測試情境：

```java
@Test void refresh_success_should_rotate_refresh_token();
@Test void old_refresh_token_should_be_rejected_after_rotation();
@Test void revoked_refresh_token_should_be_rejected();
@Test void logout_current_session_should_not_revoke_other_sessions();
```

- [ ] **Step 2: 執行 auth service 測試，確認 rotation 相關情境失敗**

Run: `mvn -q "-Dtest=AuthServiceImplTest" test`
Expected: FAIL，因現況 refresh 會重複回傳原 token。

- [ ] **Step 3: 擴充 `RefreshToken` 模型，加入最小必要欄位並寫清楚相容性決策**

目標欄位：
- token 雜湊值
- revokedAt 或等價撤銷資訊
- lastUsedAt
- session 識別值

相容性決策：
- 現有 refresh token 在第二期切換後視為失效，可接受重新登入
- 依賴 `ddl-auto: update` 演進欄位，不在本期導入 migration 工具

- [ ] **Step 4: 在 `AuthServiceImpl.refresh()` 實作 rotation**

最小流程：
1. 以收到的 refresh token 算雜湊查詢 session
2. 驗證未撤銷、未過期
3. 撤銷舊 token
4. 產生新 refresh token 與新 access token
5. 回傳新 token 組

- [ ] **Step 5: 新增 repository 測試，驗證 schema mapping 與查詢方法可用**

至少驗證：
- 可依 token 雜湊值查到 session
- 可依 session 識別值區分不同 session
- 撤銷狀態與到期條件不會造成查詢歧義

- [ ] **Step 6: 在登入與登出流程同步更新 session 資料**

目標：
- 登入建立新 session
- 登出只撤銷目前 session
- 同帳號其他 session 不受影響

- [ ] **Step 7: 重跑 `AuthServiceImplTest` 與 `RefreshTokenRepositoryTest`**

Run: `mvn -q "-Dtest=AuthServiceImplTest,RefreshTokenRepositoryTest" test`
Expected: PASS

- [ ] **Step 8: Commit refresh token rotation**

```bash
git add src/main/java/com/docflow/auth/entity/RefreshToken.java src/main/java/com/docflow/auth/repository/RefreshTokenRepository.java src/main/java/com/docflow/auth/service/AuthServiceImpl.java src/test/java/com/docflow/auth/AuthServiceImplTest.java src/test/java/com/docflow/auth/RefreshTokenRepositoryTest.java
git commit -m "feat: rotate refresh tokens per session"
```

### Task 5: 第二期對齊 controller、例外回應與前端 token 更新

**Files:**
- Modify: `src/main/java/com/docflow/auth/controller/AuthController.java`
- Modify: `src/main/java/com/docflow/common/exception/GlobalExceptionHandler.java`
- Modify: `frontend/src/stores/auth.ts`
- Modify: `frontend/src/features/auth/api.ts`
- Test: `src/test/java/com/docflow/auth/AuthControllerTest.java`
- Test: `frontend/tests/unit/auth-refresh-rotation.spec.ts`
- Test: `frontend/tests/e2e/token-expiry.spec.ts`

- [ ] **Step 1: 先補 controller 測試，驗證 refresh 會回傳新的 refresh token**

```java
@Test void refresh_should_return_new_refresh_token();
@Test void revoked_or_replayed_refresh_token_should_return_unauthorized();
@Test void logout_should_only_revoke_current_session();
```

- [ ] **Step 2: 執行 controller 測試，確認目前失敗**

Run: `mvn -q "-Dtest=AuthControllerTest" test`
Expected: FAIL，因回傳格式仍沿用舊 token。

- [ ] **Step 2.1: 新增前端單元測試，驗證 rotation 後新 refresh token 已持久化**

新增測試情境：

```ts
it('refresh 成功後應更新 localStorage 內的 refresh token')
it('使用更新後的 refresh token 再次 refresh 仍可成功')
```

- [ ] **Step 2.2: 執行前端 rotation 測試，確認目前失敗**

Run: `npm exec --prefix frontend vitest run tests/unit/auth-refresh-rotation.spec.ts`
Expected: FAIL，因目前前端尚未同步保存新的 refresh token。

- [ ] **Step 3: 更新後端 API 與例外訊息，使前端可判別 refresh 失敗類型**

目標：
- refresh 成功回傳新 token 組
- 重放或撤銷時回傳一致 401 訊息

- [ ] **Step 4: 更新前端 `auth.ts` 與 `api.ts`，確保 refresh 成功時同步保存新 refresh token**

最小方向：

```ts
const tokens = await refreshToken({ refreshToken: this.refreshToken })
this.accessToken = tokens.accessToken
this.refreshToken = tokens.refreshToken
localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken)
localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken)
```

- [ ] **Step 5: 重跑前後端相關測試**

Run: `mvn -q "-Dtest=AuthControllerTest,AuthServiceImplTest,RefreshTokenRepositoryTest" test`
Run: `npm exec --prefix frontend vitest run tests/unit/auth-refresh-rotation.spec.ts`
Run: `npm run test:e2e -- --project=chromium --grep "Token"`
Expected: PASS

- [ ] **Step 6: Commit refresh API 對齊**

```bash
git add src/main/java/com/docflow/auth/controller/AuthController.java src/main/java/com/docflow/common/exception/GlobalExceptionHandler.java frontend/src/stores/auth.ts frontend/src/features/auth/api.ts src/test/java/com/docflow/auth/AuthControllerTest.java frontend/tests/unit/auth-refresh-rotation.spec.ts frontend/tests/e2e/token-expiry.spec.ts
git commit -m "feat: align refresh api with rotated sessions"
```

### Task 6: 最終驗證與 Constitution Check（後）

**Files:**
- Verify only

- [ ] **Step 1: 執行第一期與第二期核心驗證**

Run: `npm exec --prefix frontend vitest run tests/unit/auth-store.spec.ts tests/unit/api-client.spec.ts`
Run: `npm exec --prefix frontend vitest run tests/unit/auth-refresh-rotation.spec.ts`
Run: `npm run build`
Run: `npm run test:e2e -- --project=chromium --grep "Token"`
Run: `mvn -q "-Dtest=AuthServiceImplTest,AuthControllerTest,RefreshTokenRepositoryTest" test`

- [ ] **Step 2: 再做一次 Constitution Check，確認最終交付仍符合規範**

檢查項目：
- 是否仍維持兩階段 MVP 切片
- 是否未引入第三方登入、MFA 等超出需求範圍功能
- 是否每期都能單獨驗收
- 是否成功準則都可由測試或操作情境驗證

- [ ] **Step 3: 彙整剩餘風險與後續議題**

至少記錄：
- 是否需要後續補 session 管理 UI
- 是否需要補 refresh token 清理排程
- 是否需要補更細的安全告警或 rate limiting
