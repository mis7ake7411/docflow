# DocFlow Lite Frontend

Vue 3 frontend scaffold for DocFlow Lite.

## Stack
- Vue 3
- Vite
- TypeScript
- Vue Router
- Pinia
- Axios
- Vue Query
- Element Plus

## Commands
```bash
npm install
npm run dev
npm run build
npm run test:e2e
```

## Env
Copy `.env.example` to `.env` and adjust backend API base URL if needed.

## E2E 測試
以 Playwright 針對部署站執行 smoke tests。

執行前請設定環境變數：
```bash
DOCFLOW_E2E_USERNAME=your-test-user
DOCFLOW_E2E_PASSWORD=your-test-password
PLAYWRIGHT_BASE_URL=https://docflow-hoing.zeabur.app
```

執行指令：
```bash
npm run test:e2e
```
