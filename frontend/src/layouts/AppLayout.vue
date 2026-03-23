<template>
  <div
    class="app-layout"
    :class="{
      'sidebar-collapsed': !isMobile && uiStore.sidebarCollapsed,
      'drawer-open': isMobile && uiStore.sidebarDrawerOpen,
    }"
  >
    <div
      v-if="isMobile && uiStore.sidebarDrawerOpen"
      class="sidebar-backdrop"
      @click="uiStore.closeSidebarDrawer"
    />

    <aside class="sidebar">
      <div class="brand-block">
        <div class="brand-mark">DF</div>
        <div v-show="!isSidebarContentHidden" class="brand-copy">
          <h2>DocFlow Lite</h2>
          <p>文件工作台</p>
        </div>
      </div>

      <nav class="nav-links" aria-label="主導覽">
        <RouterLink
          v-for="item in visibleNavItems"
          :key="item.path"
          :to="item.path"
          class="nav-link"
          @click="handleNavClick"
        >
          <span class="nav-badge">{{ getMenuInitial(item.meta?.menuLabel || item.meta?.title) }}</span>
          <span v-show="!isSidebarContentHidden">{{ item.meta?.menuLabel || item.meta?.title }}</span>
        </RouterLink>
      </nav>

      <div v-show="!isSidebarContentHidden" class="sidebar-footer">
        <span class="footer-label">目前角色</span>
        <strong>{{ getRoleLabel(authStore.userRole) }}</strong>
      </div>
    </aside>

    <main class="main-panel">
      <header class="shell-topbar">
        <div class="shell-topbar-left">
          <el-button text class="icon-button" @click="toggleSidebar">
            {{ isMobile ? '選單' : uiStore.sidebarCollapsed ? '展開' : '收合' }}
          </el-button>
          <PageHeader
            :title="pageTitle"
            :subtitle="pageSubtitle"
            :breadcrumb-items="breadcrumbItems"
          />
        </div>

        <div class="shell-topbar-right">
          <div class="density-switch" aria-label="內容密度設定">
            <button
              type="button"
              class="density-option"
              :class="{ active: uiStore.contentDensity === 'comfortable' }"
              @click="uiStore.setContentDensity('comfortable')"
            >
              舒適
            </button>
            <button
              type="button"
              class="density-option"
              :class="{ active: uiStore.contentDensity === 'compact' }"
              @click="uiStore.setContentDensity('compact')"
            >
              緊湊
            </button>
          </div>

          <div class="user-card">
            <div class="user-avatar">{{ (authStore.user?.username || '訪客').slice(0, 1).toUpperCase() }}</div>
            <div class="user-meta">
              <strong>{{ authStore.user?.username || '訪客' }}</strong>
              <p>{{ authStore.user?.email || '尚未登入' }}</p>
            </div>
          </div>

          <el-button type="danger" plain @click="handleLogout">登出</el-button>
        </div>
      </header>

      <div class="view-tabs">
        <div class="tab-chip">首頁</div>
        <div class="tab-chip active">{{ pageTitle }}</div>
      </div>

      <section class="content-shell">
        <slot />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { navigationRoutes } from '@/router/routes'
import { hasAnyRole } from '@/shared/auth/permissions'
import { PageHeader } from '@/shared/components'
import { getRoleLabel } from '@/shared/utils/display'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const MOBILE_BREAKPOINT = 960
const DENSITY_CLASSES = ['app-density-comfortable', 'app-density-compact']

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()
const uiStore = useUiStore()
const isMobile = ref(false)

const visibleNavItems = computed(() =>
  navigationRoutes.filter((item) => hasAnyRole(authStore.userRole ?? undefined, item.meta?.roles)),
)
const pageTitle = computed(() => route.meta.title || 'DocFlow Lite')
const pageSubtitle = computed(() => route.meta.subtitle || '集中管理文件、資料夾與活動紀錄')
const breadcrumbItems = computed(() => route.meta.breadcrumb || [pageTitle.value])
const isSidebarContentHidden = computed(() => !isMobile.value && uiStore.sidebarCollapsed)

watch(
  () => authStore.isAuthenticated,
  async (isAuthenticated) => {
    if (!isAuthenticated && router.currentRoute.value.path !== '/login') {
      await router.replace('/login')
    }
  },
)

watch(
  () => uiStore.contentDensity,
  (density) => {
    document.body.classList.remove(...DENSITY_CLASSES)
    document.body.classList.add(`app-density-${density}`)
  },
  { immediate: true },
)

onMounted(() => {
  updateViewport()
  window.addEventListener('resize', updateViewport)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateViewport)
  document.body.classList.remove(...DENSITY_CLASSES)
})

function updateViewport() {
  isMobile.value = window.innerWidth <= MOBILE_BREAKPOINT
  if (!isMobile.value) {
    uiStore.closeSidebarDrawer()
  }
}

function toggleSidebar() {
  if (isMobile.value) {
    uiStore.toggleSidebarDrawer()
    return
  }

  uiStore.toggleSidebarCollapsed()
}

function handleNavClick() {
  if (isMobile.value) {
    uiStore.closeSidebarDrawer()
  }
}

function getMenuInitial(label?: string) {
  return label?.slice(0, 1) || 'D'
}

async function handleLogout() {
  await authStore.logout()
  uiStore.closeSidebarDrawer()
  ElMessage.success('已登出')
  await router.replace('/login')
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 264px 1fr;
  background: #f2f5f9;
}

.sidebar-backdrop {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: rgba(15, 23, 42, 0.42);
}

.sidebar {
  position: relative;
  z-index: 30;
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 12px 0 0;
  background: linear-gradient(180deg, #08292b 0%, #0a222d 100%);
  color: #e5f3f5;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.sidebar-collapsed {
  grid-template-columns: 86px 1fr;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 24px 18px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #0b3347;
  background: #28a7ec;
}

.brand-copy h2,
.brand-copy p {
  margin: 0;
}

.brand-copy p {
  margin-top: 6px;
  color: rgba(229, 243, 245, 0.65);
  font-size: 0.92rem;
}

.nav-links {
  display: flex;
  flex-direction: column;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 2px;
  padding: 18px 24px;
  color: #d4eef1;
  text-decoration: none;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.nav-link:hover {
  background: rgba(92, 208, 214, 0.12);
}

.nav-link.router-link-active {
  color: #ffffff;
  background: #5ccfd6;
}

.nav-link.router-link-active .nav-badge {
  background: rgba(255, 255, 255, 0.18);
}

.nav-badge {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.1);
  font-size: 0.88rem;
  font-weight: 700;
}

.sidebar-footer {
  margin-top: auto;
  padding: 20px 24px 28px;
  color: rgba(229, 243, 245, 0.82);
}

.footer-label {
  display: block;
  margin-bottom: 8px;
  color: rgba(229, 243, 245, 0.65);
  font-size: 0.9rem;
}

.main-panel {
  min-width: 0;
  padding: 0;
}

.shell-topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 18px 28px;
  background: #ffffff;
  border-bottom: 1px solid #dfe6ee;
}

.shell-topbar-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  min-width: 0;
}

.shell-topbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.icon-button {
  margin-top: 2px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.header-icon {
  width: 34px;
  height: 34px;
  border: 1px solid #d9e1ea;
  border-radius: 10px;
  background: #fff;
  color: #526071;
  cursor: pointer;
}

.density-switch {
  display: inline-flex;
  padding: 4px;
  border-radius: 999px;
  background: #eef2f6;
}

.density-option {
  border: 0;
  background: transparent;
  color: #526071;
  border-radius: 999px;
  padding: 8px 14px;
  font: inherit;
  cursor: pointer;
}

.density-option.active {
  background: #ffffff;
  color: #122033;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08);
}

.user-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: #dbeafe;
  color: #1d4ed8;
  font-weight: 700;
}

.user-meta strong,
.user-meta p {
  margin: 0;
}

.user-meta p {
  color: #748092;
  font-size: 0.88rem;
}

.view-tabs {
  display: flex;
  gap: 10px;
  align-items: center;
  padding: 10px 28px;
  background: #ffffff;
  border-bottom: 1px solid #e5ebf1;
}

.tab-chip {
  padding: 8px 14px;
  border: 1px solid #dfe6ee;
  background: #fafcff;
  color: #526071;
}

.tab-chip.active {
  border-bottom: 2px solid #41c8cf;
  color: #17b8c1;
  background: #ffffff;
}

.content-shell {
  padding: 28px;
}

@media (max-width: 960px) {
  .app-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: min(300px, 86vw);
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }

  .drawer-open .sidebar {
    transform: translateX(0);
  }

  .shell-topbar,
  .view-tabs,
  .content-shell {
    padding-inline: 16px;
  }
}

@media (max-width: 768px) {
  .shell-topbar {
    flex-direction: column;
    align-items: stretch;
  }

  .shell-topbar-left,
  .shell-topbar-right {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-start;
  }
}
</style>
