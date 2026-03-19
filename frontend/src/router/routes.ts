import type { RouteRecordRaw } from 'vue-router'
import DashboardPage from '@/pages/DashboardPage.vue'
import DocumentDetailPage from '@/pages/DocumentDetailPage.vue'
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
      subtitle: '管理資料夾與文件內容',
      breadcrumb: ['工作台', '儀表板'],
      requiresAuth: true,
      roles: ['USER', 'ADMIN'],
      menu: true,
      menuLabel: '儀表板',
    },
  },
  {
    path: '/app/documents/:id',
    name: 'document-detail',
    component: DocumentDetailPage,
    meta: {
      title: '文件明細',
      subtitle: '檢視文件內容與附件檔案操作',
      breadcrumb: ['工作台', '文件明細'],
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
      breadcrumb: ['工作台', '洞察報表'],
      requiresAuth: true,
      roles: ['USER', 'ADMIN'],
      menu: true,
      menuLabel: '洞察報表',
    },
  },
]

export const navigationRoutes = appRoutes.filter((route) => route.meta?.menu)
