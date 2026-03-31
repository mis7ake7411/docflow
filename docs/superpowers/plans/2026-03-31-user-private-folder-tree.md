# USER 私有資料夾樹 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 讓 `USER` 在檔案管理頁只看到自己建立的資料夾樹，且資料夾操作、排序與文件掛載都只能作用在自己的資料夾集合。

**Architecture:** 以既有資料夾模型為基礎，不新增新的可視範圍欄位，而是依角色收斂資料夾樹查詢範圍與操作授權。後端先補齊資料夾與文件掛載的 owner 驗證，再讓前端以新的私有資料夾樹行為調整畫面文案與狀態同步，避免只改 UI 造成 API 可繞過。

**Tech Stack:** Spring Boot, Spring Data JPA, JUnit 5, Vue 3, Pinia, Vue Query, Playwright

---

## Constitution Check

- 與使用者價值一致：是。本計畫直接解決 `USER` 在共享資料夾樹中排序語意混亂與可見範圍不清的問題。
- 是否可獨立驗收：是。完成後可由 `USER A` / `USER B` / `ADMIN` 三種情境獨立驗收。
- 是否避免過度設計：是。不新增 `private/public` 通用模型，也不引入個人化排序架構。
- 是否含實作細節於規格：否。實作細節只存在本計畫，規格維持使用者價值與驗收條件。

## 檔案結構與責任

- Modify: `src/main/java/com/docflow/folder/repository/FolderRepository.java`
  - 新增依建立者篩選的資料夾樹與同層資料夾查詢
- Modify: `src/main/java/com/docflow/folder/service/FolderServiceImpl.java`
  - 實作角色化資料夾樹查詢、資料夾 owner 驗證、父節點驗證、排序範圍調整
- Modify: `src/main/java/com/docflow/document/service/DocumentServiceImpl.java`
  - 補齊文件掛載資料夾的 owner / privileged 驗證
- Test: `src/test/java/com/docflow/folder/FolderServiceImplTest.java`
  - 補資料夾樹、父節點與 owner 驗證測試
- Test: `src/test/java/com/docflow/folder/FolderControllerTest.java`
  - 覆蓋資料夾樹 API 成功情境，必要時補角色差異 mock
- Test: `src/test/java/com/docflow/document/DocumentServiceImplPermissionTest.java`
  - 補文件不可掛到他人資料夾的測試
- Modify: `frontend/src/features/folder/components/FolderTree.vue`
  - 以私有資料夾樹為前提調整樹狀顯示、文案與失效選取回退
- Modify: `frontend/src/features/folder/components/FolderFormDialog.vue`
  - 父節點選項隨私有資料夾樹同步，避免失效父節點殘留
- Modify: `frontend/src/features/document/components/DocumentTable.vue`
  - 當 `selectedFolderId` 不在當前樹中時回退到全部文件
- Modify: `frontend/src/stores/ui.ts`
  - 視需要補輔助 action，支援清除失效資料夾選取
- Test: `frontend/tests/e2e/permission-ui.spec.ts`
  - 改寫為 `USER` 看不到他人資料夾

## 複雜度追蹤表

| 項目 | 是否新增 | 原因 | 拒絕的替代方案 |
| --- | --- | --- | --- |
| 依角色切換資料夾樹查詢 | 必要 | 直接對應 `USER` 私有資料夾樹需求 | 單改前端隱藏，會留下 API 授權漏洞 |
| 文件掛載資料夾 owner 驗證 | 必要 | 避免 `USER` 透過 API 掛到他人資料夾 | 僅依前端下拉限制，不足以防繞過 |
| 失效資料夾選取回退 | 必要 | 避免 UI 殘留不存在的 folder filter | 忽略狀態同步，會造成列表與標題不一致 |
| `private/public` 通用模型 | 否 | 超出 MVP，且本需求不需要 | 現階段不做 |

## 既有異常資料最低處理原則

- 本次實作不負責修正既有跨使用者父子關係或文件掛載到他人資料夾的歷史資料
- 本次實作必須保證「不再產生新的跨使用者父子關係或文件掛載異常」
- 若歷史異常資料導致 `USER` 在私有資料夾樹中看不到某些舊節點，視為可接受；需在最終驗證中記錄風險，但不在本次範圍內擴充資料修復機制
- `ADMIN` / `MANAGER` 仍需可透過既有高權限行為查看與處理這些資料

---

### Task 1: 後端資料夾樹改為 USER 私有可見

**Files:**
- Modify: `src/main/java/com/docflow/folder/repository/FolderRepository.java`
- Modify: `src/main/java/com/docflow/folder/service/FolderServiceImpl.java`
- Test: `src/test/java/com/docflow/folder/FolderServiceImplTest.java`

- [ ] **Step 1: 先寫失敗測試，驗證 USER 讀取資料夾樹時只會拿到自己建立的資料夾**

- [ ] **Step 1.1: 補一個成功測試，驗證 `ADMIN` / `MANAGER` 讀取資料夾樹時仍可取得既有全量資料夾**

- [ ] **Step 2: 執行單一測試，確認目前失敗原因是資料夾樹仍採全量未刪除資料夾**

Run: `mvn -q "-Dtest=FolderServiceImplTest" test`

- [ ] **Step 3: 在 repository 新增依建立者篩選的資料夾樹查詢，保留 privileged role 既有全量查詢**

- [ ] **Step 4: 在 service 的 `getTree()` 依角色切換查詢來源，讓 USER 只組自己的資料夾樹**

- [ ] **Step 5: 重新執行資料夾 service 測試，確認資料夾樹行為符合新規則**

Run: `mvn -q "-Dtest=FolderServiceImplTest" test`

- [ ] **Step 6: Commit 後端資料夾樹查詢調整**

### Task 2: 後端補齊資料夾 owner 與父節點驗證

**Files:**
- Modify: `src/main/java/com/docflow/folder/service/FolderServiceImpl.java`
- Test: `src/test/java/com/docflow/folder/FolderServiceImplTest.java`

- [ ] **Step 1: 先寫失敗測試，覆蓋以下情境**

情境：
- `USER` 不可把資料夾建立在他人父節點下
- `USER` 不可將自己的資料夾移到他人父節點下
- `USER` 不可編輯他人資料夾
- `USER` 不可刪除他人資料夾
- `USER` 在沒有任何資料夾時，仍可成功建立第一個根資料夾

- [ ] **Step 2: 執行單一測試，確認目前失敗原因是 service 只驗資料夾存在、未驗 owner**

Run: `mvn -q "-Dtest=FolderServiceImplTest" test`

- [ ] **Step 3: 在 service 抽出共用 owner / privileged 驗證邏輯**

- [ ] **Step 4: 將父節點解析改為同時驗證「存在、未刪除、當前使用者可掛載」**

- [ ] **Step 5: 在 `update` 與 `delete` 入口補 `USER` 僅能操作自己資料夾的檢查**

- [ ] **Step 6: 重跑資料夾 service 測試，確認 create/update/delete 權限與父節點規則通過**

Run: `mvn -q "-Dtest=FolderServiceImplTest" test`

- [ ] **Step 7: Commit 資料夾 owner 與父節點驗證**

### Task 3: 後端讓排序只作用在 USER 自己的同層資料夾

**Files:**
- Modify: `src/main/java/com/docflow/folder/repository/FolderRepository.java`
- Modify: `src/main/java/com/docflow/folder/service/FolderServiceImpl.java`
- Test: `src/test/java/com/docflow/folder/FolderServiceImplTest.java`

- [ ] **Step 1: 先寫失敗測試，驗證 USER 重排時只接受自己的同層資料夾集合**

- [ ] **Step 2: 執行單一測試，確認目前失敗原因是同層集合仍依全量 sibling 載入**

Run: `mvn -q "-Dtest=FolderServiceImplTest" test`

- [ ] **Step 3: 調整同層資料夾載入方式，使 USER 與 privileged role 使用一致但角色化的 sibling 集合**

- [ ] **Step 4: 保留完整 sibling set 驗證，但集合應以目前使用者可見範圍為準**

- [ ] **Step 5: 重跑資料夾相關測試，確認排序只改到自己的資料夾**

Run: `mvn -q "-Dtest=FolderServiceImplTest,FolderControllerTest" test`

- [ ] **Step 6: Commit 排序邏輯調整**

### Task 4: 後端補齊文件掛載資料夾驗證

**Files:**
- Modify: `src/main/java/com/docflow/document/service/DocumentServiceImpl.java`
- Test: `src/test/java/com/docflow/document/DocumentServiceImplPermissionTest.java`

- [ ] **Step 1: 先寫失敗測試，驗證 USER 建立文件與更新文件時不可指定他人資料夾**

- [ ] **Step 2: 執行單一測試，確認目前失敗原因是 `resolveFolder()` 僅驗證資料夾存在**

Run: `mvn -q "-Dtest=DocumentServiceImplPermissionTest" test`

- [ ] **Step 3: 在文件 service 抽出資料夾可用性驗證，讓 USER 只能掛到自己的資料夾，privileged role 維持既有行為**

- [ ] **Step 4: 重跑文件權限測試，確認建立與更新文件皆遵守新規則**

Run: `mvn -q "-Dtest=DocumentServiceImplPermissionTest" test`

- [ ] **Step 5: Commit 文件掛載資料夾驗證**

### Task 5: 前端調整為私有資料夾樹體驗

**Files:**
- Modify: `frontend/src/features/folder/components/FolderTree.vue`
- Modify: `frontend/src/features/folder/components/FolderFormDialog.vue`
- Modify: `frontend/src/features/document/components/DocumentTable.vue`
- Modify: `frontend/src/stores/ui.ts`
- Test: `frontend/tests/e2e/permission-ui.spec.ts`

- [ ] **Step 1: 先更新 `frontend/tests/e2e/permission-ui.spec.ts`，改為驗證 USER 看不到他人資料夾**

- [ ] **Step 2: 在同一支 E2E 中加入失效選取回退驗證，確認畫面會回到全部文件，且不殘留失效資料夾名稱或 `資料夾 #id` 識別**

- [ ] **Step 3: 調整資料夾樹文案，讓節點數、空狀態與頁面說明明確對應「自己的資料夾」**

- [ ] **Step 4: 保留既有資料夾編輯/刪除操作，但移除依賴「看到他人節點但 disabled」的預設**

- [ ] **Step 5: 當目前 `selectedFolderId` 不在新樹中時，自動回退到全部文件，並清掉畫面上的失效資料夾識別**

- [ ] **Step 6: 確認資料夾表單父節點選項在私有資料夾樹下仍可正確建立與編輯，且零資料夾時仍可建立第一個根資料夾**

- [ ] **Step 7: 執行前端建置，確認型別與狀態同步皆正常**

Run: `npm run build`

- [ ] **Step 8: 執行 E2E 驗證前端私有資料夾樹行為**

Run: `npm run test:e2e -- --project=chromium --grep "文件與資料夾權限 UI"`

- [ ] **Step 9: Commit 前端私有資料夾樹調整**

### Task 6: 端對端驗證與收尾

**Files:**
- Test: `frontend/tests/e2e/permission-ui.spec.ts`
- Verify only

- [ ] **Step 1: 補或更新 E2E 驗收案例**

驗證重點：
- `USER A` 看不到 `USER B` 的資料夾
- `USER A` 仍可正常建立自己的資料夾與文件
- 當選取資料夾失效時，畫面自動回到全部文件，且不殘留失效資料夾名稱或編號
- `ADMIN` / `MANAGER` 的既有高權限資料夾可視範圍不退化
- 共享文件列表、文件詳情與文件分享授權行為不退化

- [ ] **Step 2: 執行後端相關測試**

Run: `mvn -q "-Dtest=FolderServiceImplTest,FolderControllerTest,DocumentServiceImplPermissionTest" test`

- [ ] **Step 3: 執行前端建置與必要 E2E**

Run: `npm run build`
Run: `npm run test:e2e -- --project=chromium --grep "文件與資料夾權限 UI"`

- [ ] **Step 3.1: 驗證 `ADMIN` / `MANAGER` 與文件分享不變項**

驗證方式：
- 執行既有後端權限測試，確認文件分享與文件詳情授權未退化
- 若有對應 E2E 或手動驗證路徑，確認共享文件頁與文件詳情頁仍可正常使用

Run: `mvn -q "-Dtest=DocumentServiceImplPermissionTest" test`

- [ ] **Step 4: 再做一次 Constitution Check，確認最終交付仍符合 MVP 與可驗收原則**

- [ ] **Step 5: 彙整風險與後續議題**

至少列出：
- 既有跨使用者父子關係資料是否需要資料修正
- 是否保留 `ADMIN` / `MANAGER` 全量可視行為
- 未來若要共享資料夾，再另立規格，不併入本次

- [ ] **Step 6: Commit 最終驗證與收尾**
