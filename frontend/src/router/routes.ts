import type { RouteRecordRaw } from 'vue-router'
import DashboardPage from '@/pages/DashboardPage.vue'
import DocumentDetailPage from '@/pages/DocumentDetailPage.vue'
import FileManagementPage from '@/pages/FileManagementPage.vue'
import InsightsPage from '@/pages/InsightsPage.vue'
import LoginPage from '@/pages/LoginPage.vue'

export type AppRole = 'USER' | 'ADMIN'

export interface AppRouteMeta {
  title: string
  subtitle?: string
  breadcrumb?: string[]
  requiresAuth?: boolean
  publicOnly?: boolean
  roles?: AppRole[]
  menu?: boolean
  menuLabel?: string
}

declare module 'vue-router' {
  interface RouteMeta extends AppRouteMeta {}
}

export const appRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login',
  },
  {
    path: '/login',
    name: 'login',
    component: LoginPage,
    meta: {
      title: '登入',
      subtitle: '使用帳號密碼登入 DocFlow Lite',
      breadcrumb: ['登入'],
      publicOnly: true,
    },
  },
  {
    path: '/app',
    name: 'dashboard',
    component: DashboardPage,
    meta: {
      title: '儀表板',
      subtitle: '總覽文件狀態與使用趨勢',
      breadcrumb: ['首頁', '儀表板'],
      requiresAuth: true,
      roles: ['USER', 'ADMIN'],
      menu: true,
      menuLabel: '儀表板',
    },
  },
  {
    path: '/app/files',
    name: 'file-management',
    component: FileManagementPage,
    meta: {
      title: '文件管理',
      subtitle: '集中管理資料夾與文件內容',
      breadcrumb: ['首頁', '文件管理'],
      requiresAuth: true,
      roles: ['USER', 'ADMIN'],
      menu: true,
      menuLabel: '文件管理',
    },
  },
  {
    path: '/app/documents/:id',
    name: 'document-detail',
    component: DocumentDetailPage,
    meta: {
      title: '文件詳情',
      subtitle: '查看文件內容、上傳附件與下載檔案',
      breadcrumb: ['首頁', '文件詳情'],
      requiresAuth: true,
      roles: ['USER', 'ADMIN'],
    },
  },
  {
    path: '/app/insights',
    name: 'insights',
    component: InsightsPage,
    meta: {
      title: '洞察報表',
      subtitle: '檢視熱門文件、最近瀏覽與活動紀錄',
      breadcrumb: ['首頁', '洞察報表'],
      requiresAuth: true,
      roles: ['USER', 'ADMIN'],
      menu: true,
      menuLabel: '洞察報表',
    },
  },
]

export const navigationRoutes = appRoutes.filter((route) => route.meta?.menu)
