# DocFlow Lite Frontend

DocFlow Lite 的 Vue 3 前端專案。

## 技術棧
- Vue 3
- Vite
- TypeScript
- Vue Router
- Pinia
- Axios
- Vue Query
- Element Plus
- Playwright

## 常用指令
```bash
npm install
npm run dev
npm run build
npm run test:e2e
```

## 環境變數
將 `frontend/.env.example` 複製為 `frontend/.env`，再依需求調整。

本機開發常用設定：
```bash
VITE_API_BASE_URL=http://localhost:8080
```

## 本機 E2E 測試
目前分享與權限 UI 的 Playwright 測試，已改成會自行註冊測試帳號，不需要事先準備固定帳號。

### 前置條件
1. 前端已啟動於 `http://localhost:5173`
2. 後端 API 已啟動於 `http://localhost:8080`
3. `frontend/.env` 的 `VITE_API_BASE_URL` 指向本機後端

### Playwright 預設行為
- 若未設定 `PLAYWRIGHT_BASE_URL`，Playwright 會預設連到 `http://localhost:5173`
- 若未設定 `DOCFLOW_E2E_API_BASE_URL`，E2E helper 會預設使用 `http://localhost:8080`

### 可覆蓋的環境變數
```bash
PLAYWRIGHT_BASE_URL=http://localhost:5173
DOCFLOW_E2E_API_BASE_URL=http://localhost:8080
```

### 執行全部 E2E
```bash
npm run test:e2e
```

### 只執行文件分享測試
```bash
npm run test:e2e -- --project=chromium --grep "文件分享"
```

### 只執行文件與資料夾權限 UI 測試
```bash
npm run test:e2e -- --project=chromium --grep "文件與資料夾權限 UI"
```

### 已驗證的本機測試組合
```bash
npm run test:e2e -- --project=chromium --grep "文件分享|文件與資料夾權限 UI"
```

以上組合目前可通過：
- 文件分享：3 支
- 文件與資料夾權限 UI：4 支

## 測試結構
- `tests/e2e/document-share.spec.ts`
  - 文件分享建立
  - 分享權限升級
  - 取消分享與活動紀錄
- `tests/e2e/permission-ui.spec.ts`
  - `VIEW` 分享文件在列表列的 disabled 狀態
  - `VIEW` 分享文件在詳情頁的 disabled 狀態
  - 被分享者不可管理分享的負向案例
  - 他人資料夾的 disabled 狀態
- `tests/e2e/helpers/docflow.ts`
  - 共用登入 / 註冊 / 建立文件 / 建立分享 / 建立資料夾 helper
