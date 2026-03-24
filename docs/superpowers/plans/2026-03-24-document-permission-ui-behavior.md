# 文件/資料夾權限 UI 行為 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在資料夾列表、文件列表、文件詳情中，依 `createdBy` 與使用者角色控制編輯/上傳/刪除按鈕的可用性並給出一致提示。

**Architecture:** 在前端新增權限判斷與訊息工具，於 FolderTree、DocumentTable、DocumentDetailPage 套用一致的 disabled + tooltip + toast；所有 API 操作仍以後端 403 為最終裁決並在 UI 顯示提示。

**Tech Stack:** Vue 3, Element Plus, Pinia, Vue Query, Axios, Playwright

---

**檔案結構與責任分配**
- 建立：`frontend/src/shared/utils/permission.ts`
- 修改：`frontend/src/features/folder/components/FolderTree.vue`
- 修改：`frontend/src/features/document/components/DocumentTable.vue`
- 修改：`frontend/src/pages/DocumentDetailPage.vue`
- 修改：`frontend/src/features/document/components/DocumentUploadDialog.vue`
- 修改：`frontend/src/features/document/components/DocumentFormDialog.vue`
- 修改：`frontend/src/features/folder/components/FolderFormDialog.vue`
- 新增測試：`frontend/tests/e2e/permission-ui.spec.ts`

---

### Task 1: 建立權限判斷與訊息工具

**Files:**
- Create: `frontend/src/shared/utils/permission.ts`

- [ ] **Step 1: 撰寫失敗測試（Playwright 行為骨架）**

```ts
// frontend/tests/e2e/permission-ui.spec.ts
import { expect, test, type Page } from '@playwright/test'

const ownerUser = process.env.DOCFLOW_E2E_OWNER_USERNAME
const ownerPass = process.env.DOCFLOW_E2E_OWNER_PASSWORD
const otherUser = process.env.DOCFLOW_E2E_OTHER_USERNAME
const otherPass = process.env.DOCFLOW_E2E_OTHER_PASSWORD

test.describe('文件/資料夾權限 UI', () => {
  test.skip(!ownerUser || !ownerPass || !otherUser || !otherPass, '需提供兩組帳號')

  async function login(page: Page, username: string, password: string) {
    await page.goto('/login')
    await page.getByRole('textbox', { name: 'Username' }).fill(username)
    await page.getByRole('textbox', { name: 'Password' }).fill(password)
    await page.getByRole('button', { name: '登入' }).click()
    await expect(page).toHaveURL(/\/app/)
  }

  test('他人文件按鈕為 disabled 並顯示提示', async ({ page }) => {
    await login(page, otherUser!, otherPass!)
    await page.getByRole('link', { name: '文件管理' }).click()

    // 先確認表格存在
    await expect(page.getByRole('heading', { name: '文件列表' })).toBeVisible()

    // TODO: 依實作選擇一筆非本人文件列，確認「編輯/上傳/刪除」為 disabled 並有提示
  })
})
```

- [ ] **Step 2: 執行測試確認失敗**

Run: `npm run test:e2e -- --project=chromium --grep "文件/資料夾權限 UI"`
Expected: FAIL（測試尚未完成或 UI 尚未實作）

- [ ] **Step 3: 建立權限工具**

```ts
// frontend/src/shared/utils/permission.ts
import type { UserSummary } from '@/shared/types/auth'

export const PERMISSION_MESSAGES = {
  folderHint: '僅能修改自己建立的資料夾',
  documentHint: '僅能修改自己建立的文件',
  folderForbidden: '無權限操作此資料夾',
  documentForbidden: '無權限操作此文件',
}

export function isAdminOrManager(user: UserSummary | null) {
  return user?.role === 'ADMIN' || user?.role === 'MANAGER'
}

export function canModifyResource(createdBy: number | null | undefined, user: UserSummary | null) {
  if (isAdminOrManager(user)) {
    return true
  }
  if (!user || createdBy == null) {
    return false
  }
  return createdBy === user.id
}
```

- [ ] **Step 4: Commit**

```bash
git add frontend/src/shared/utils/permission.ts frontend/tests/e2e/permission-ui.spec.ts

git commit -m "feat: add UI permission helpers"
```

---

### Task 2: 資料夾列表權限狀態（FolderTree）

**Files:**
- Modify: `frontend/src/features/folder/components/FolderTree.vue`
- Modify: `frontend/src/features/folder/components/FolderFormDialog.vue`

- [ ] **Step 1: 撰寫失敗測試（針對資料夾操作 disabled）**

```ts
// frontend/tests/e2e/permission-ui.spec.ts（補充）
// TODO: 選擇一個非本人資料夾節點，驗證「編輯/刪除」disabled + tooltip
```

- [ ] **Step 2: 執行測試確認失敗**

Run: `npm run test:e2e -- --project=chromium --grep "文件/資料夾權限 UI"`
Expected: FAIL

- [ ] **Step 3: 實作資料夾權限 UI + 403 提示**

```vue
<!-- FolderTree.vue 節點按鈕（示意） -->
<el-tooltip v-if="!canEditFolder(data)" :content="PERMISSION_MESSAGES.folderHint">
  <span>
    <el-button text size="small" disabled @click.stop>編輯</el-button>
  </span>
</el-tooltip>
<el-button v-else text size="small" @click.stop="openEditDialog(data)">編輯</el-button>
```

```vue
<!-- 刪除同理：disabled 時不顯示 popconfirm -->
<el-tooltip v-if="!canEditFolder(data)" :content="PERMISSION_MESSAGES.folderHint">
  <span><el-button text size="small" type="danger" disabled>刪除</el-button></span>
</el-tooltip>
<el-popconfirm v-else title="確定刪除這個資料夾？" @confirm="handleDelete(data.id)"> ... </el-popconfirm>
```

在 `FolderTree.vue` 的 `deleteMutation.onError` 與 `FolderFormDialog.vue` 的 update mutation 補 403 提示：

```ts
if (isAxiosError(error) && error.response?.status === 403) {
  ElMessage.error(PERMISSION_MESSAGES.folderForbidden)
  return
}
```

- [ ] **Step 4: 重新執行測試確認通過**

Run: `npm run test:e2e -- --project=chromium --grep "文件/資料夾權限 UI"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/folder/components/FolderTree.vue \
        frontend/src/features/folder/components/FolderFormDialog.vue

git commit -m "feat: enforce folder permission UI"
```

---

### Task 3: 文件列表權限狀態（DocumentTable）

**Files:**
- Modify: `frontend/src/features/document/components/DocumentTable.vue`
- Modify: `frontend/src/features/document/components/DocumentUploadDialog.vue`

- [ ] **Step 1: 撰寫失敗測試（文件列表 edit/upload/delete disabled）**

```ts
// frontend/tests/e2e/permission-ui.spec.ts（補充）
// TODO: 針對非本人文件，驗證「編輯/上傳/刪除」disabled + tooltip
```

- [ ] **Step 2: 執行測試確認失敗**

Run: `npm run test:e2e -- --project=chromium --grep "文件/資料夾權限 UI"`
Expected: FAIL

- [ ] **Step 3: 實作文件列表權限 UI + 403 提示**

```vue
<!-- DocumentTable.vue：新增上傳按鈕（僅針對既有文件） -->
<el-tooltip v-if="!canEditDocument(scope.row)" :content="PERMISSION_MESSAGES.documentHint">
  <span><el-button text disabled>上傳</el-button></span>
</el-tooltip>
<el-button v-else text @click="openUploadDialog(scope.row)">上傳</el-button>
```

```ts
// DocumentTable.vue script
const uploadDialogVisible = ref(false)
const uploadingDocumentId = ref<number | null>(null)

function openUploadDialog(document: DocumentItem) {
  uploadingDocumentId.value = document.id
  uploadDialogVisible.value = true
}

function closeUploadDialog() {
  uploadDialogVisible.value = false
  uploadingDocumentId.value = null
}
```

```vue
<DocumentUploadDialog
  v-if="uploadingDocumentId"
  v-model="uploadDialogVisible"
  :document-id="uploadingDocumentId"
  @update:modelValue="closeUploadDialog"
/>
```

在 `deleteMutation.onError` 與 `DocumentUploadDialog.vue` 的 upload mutation `onError` 補 403 提示：

```ts
if (isAxiosError(error) && error.response?.status === 403) {
  ElMessage.error(PERMISSION_MESSAGES.documentForbidden)
}
```

- [ ] **Step 4: 重新執行測試確認通過**

Run: `npm run test:e2e -- --project=chromium --grep "文件/資料夾權限 UI"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/features/document/components/DocumentTable.vue \
        frontend/src/features/document/components/DocumentUploadDialog.vue

git commit -m "feat: enforce document list permission UI"
```

---

### Task 4: 文件詳情權限狀態（DocumentDetailPage）

**Files:**
- Modify: `frontend/src/pages/DocumentDetailPage.vue`
- Modify: `frontend/src/features/document/components/DocumentFormDialog.vue`

- [ ] **Step 1: 撰寫失敗測試（文件詳情操作 disabled + 提示）**

```ts
// frontend/tests/e2e/permission-ui.spec.ts（補充）
// TODO: 進入非本人文件詳情，驗證「編輯/上傳/刪除」disabled + 提示文案
```

- [ ] **Step 2: 執行測試確認失敗**

Run: `npm run test:e2e -- --project=chromium --grep "文件/資料夾權限 UI"`
Expected: FAIL

- [ ] **Step 3: 實作文件詳情權限 UI + 刪除功能 + 403 提示**

```vue
<el-tooltip v-if="!canEditDocument(document)" :content="PERMISSION_MESSAGES.documentHint">
  <span><el-button disabled>編輯</el-button></span>
</el-tooltip>
```

```vue
<el-tooltip v-if="!canEditDocument(document)" :content="PERMISSION_MESSAGES.documentHint">
  <span><el-button type="primary" disabled>上傳</el-button></span>
</el-tooltip>
```

新增「刪除」按鈕（含 popconfirm），並在刪除 mutation `onError` 補 403 提示。

並在按鈕群組下方顯示輔助說明：

```vue
<p v-if="document && !canEditDocument(document)" class="muted">你僅能修改自己建立的文件</p>
```

在 `DocumentFormDialog.vue` 的 update mutation `onError` 補 403 提示：

```ts
if (isAxiosError(error) && error.response?.status === 403) {
  ElMessage.error(PERMISSION_MESSAGES.documentForbidden)
  return
}
```

- [ ] **Step 4: 重新執行測試確認通過**

Run: `npm run test:e2e -- --project=chromium --grep "文件/資料夾權限 UI"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/DocumentDetailPage.vue \
        frontend/src/features/document/components/DocumentFormDialog.vue

git commit -m "feat: enforce document detail permission UI"
```

---

### Task 5: 補齊測試（完整權限矩陣）

**Files:**
- Modify: `frontend/tests/e2e/permission-ui.spec.ts`

- [ ] **Step 1: 完成測試案例**

```ts
// 補齊：
// 1) 他人文件列表按鈕 disabled + tooltip
// 2) 他人文件詳情按鈕 disabled + 輔助說明
// 3) 他人資料夾編輯/刪除 disabled + tooltip
```

- [ ] **Step 2: 執行測試確認通過**

Run: `npm run test:e2e -- --project=chromium --grep "文件/資料夾權限 UI"`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add frontend/tests/e2e/permission-ui.spec.ts

git commit -m "test: add permission UI e2e coverage"
```

---

**整體驗證**
- [ ] Run: `npm run test:e2e -- --project=chromium --grep "文件/資料夾權限 UI"`
Expected: PASS
