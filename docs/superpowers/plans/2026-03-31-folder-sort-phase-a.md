# Folder Sort Phase A Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 讓資料夾建立時由系統自動指派排序，並在文件管理頁支援同層資料夾拖曳排序。

**Architecture:** 保留 `folders.sort_order` 作為共用排序欄位。後端新增同層重排 API 並在單一交易內重寫 sibling 順序；前端移除手動排序輸入，改由樹狀拖曳觸發重排請求。第一階段不支援跨層移動與個人化排序。

**Tech Stack:** Spring Boot, Spring Data JPA, Vue 3, Element Plus, Vue Query

---

### Task 1: 後端自動排序與重排 API

**Files:**
- Modify: `src/main/java/com/docflow/folder/dto/CreateFolderRequest.java`
- Modify: `src/main/java/com/docflow/folder/dto/UpdateFolderRequest.java`
- Modify: `src/main/java/com/docflow/folder/controller/FolderController.java`
- Modify: `src/main/java/com/docflow/folder/service/FolderService.java`
- Modify: `src/main/java/com/docflow/folder/service/FolderServiceImpl.java`
- Modify: `src/main/java/com/docflow/folder/repository/FolderRepository.java`
- Create: `src/main/java/com/docflow/folder/dto/ReorderFoldersRequest.java`
- Test: `src/test/java/com/docflow/folder/FolderControllerTest.java`
- Test: `src/test/java/com/docflow/folder/FolderServiceImplTest.java`

- [ ] Step 1: 先寫 service 測試，驗證建立資料夾時會自動取同層下一個排序值。
- [ ] Step 2: 執行單一測試，確認目前失敗原因是 `sortOrder` 仍由 request 提供。
- [ ] Step 3: 實作 repository 查詢與 service 邏輯，讓 create 自動算排序值。
- [ ] Step 4: 寫 service 測試，驗證 reorder API 只能重排同一 `parentId` 下的完整 sibling 集合。
- [ ] Step 5: 執行測試確認失敗，再補 controller、DTO、service 介面與 transaction 內批次更新。
- [ ] Step 6: 執行資料夾相關後端測試，確認 create/update/tree/reorder 全部通過。
- [ ] Step 7: Commit 後端變更。

### Task 2: 前端表單與拖曳排序

**Files:**
- Modify: `frontend/src/features/folder/api.ts`
- Modify: `frontend/src/features/folder/components/FolderFormDialog.vue`
- Modify: `frontend/src/features/folder/components/FolderTree.vue`
- Optional: `frontend/package.json`
- Test/Verify: `frontend/tests/e2e/...`（若現有 E2E 能覆蓋）

- [ ] Step 1: 調整前端型別，讓新增與編輯 payload 不再要求手動輸入排序。
- [ ] Step 2: 修改資料夾表單，移除排序欄位，只保留名稱與上層資料夾。
- [ ] Step 3: 在樹狀列表加入同層拖曳 UI，只允許 reorder，不允許跨層 drop。
- [ ] Step 4: 拖曳完成後送出 reorder API，成功後刷新樹狀資料，失敗時回復畫面並提示。
- [ ] Step 5: 執行 `npm run build` 驗證前端型別與打包通過。
- [ ] Step 6: Commit 前端變更。

### Task 3: 整合驗證

**Files:**
- Verify only

- [ ] Step 1: 執行後端資料夾相關測試。
- [ ] Step 2: 執行前端 build。
- [ ] Step 3: 檢查工作樹 diff，確認僅包含第一階段 A 範圍。
- [ ] Step 4: 彙整風險與後續可擴充點，特別註記方案 B 的切入點。
