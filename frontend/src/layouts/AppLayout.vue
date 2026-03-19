<template>
  <div
    class="app-layout"
    :class="{
      'is-mobile': isMobile,
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
          <p class="muted">文件工作台</p>
        </div>
      </div>

      <nav class="nav-links" aria-label="Main navigation">
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
        <p class="muted">目前角色</p>
        <strong>{{ authStore.userRole || '訪客' }}</strong>
      </div>
    </aside>

    <main class="main-panel">
      <header class="topbar page-card">
        <div class="topbar-main">
          <div class="toolbar-row">
            <el-button text class="toolbar-button" @click="toggleSidebar">
              {{ isMobile ? '選單' : uiStore.sidebarCollapsed ? '展開側欄' : '收合側欄' }}
            </el-button>

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
          </div>

          <PageHeader
            :title="pageTitle"
            :subtitle="pageSubtitle"
            :breadcrumb-items="breadcrumbItems"
          />
        </div>

        <div class="topbar-actions">
          <div class="user-card">
            <strong>{{ authStore.user?.username || 'Guest' }}</strong>
            <p class="muted">{{ authStore.user?.email || 'Not logged in' }}</p>
            <p v-if="authStore.userRole" class="role-chip">{{ authStore.userRole }}</p>
          </div>
          <el-button type="danger" plain @click="handleLogout">登出</el-button>
        </div>
      </header>

      <section>
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
  return label?.slice(0, 1).toUpperCase() || 'D'
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
  position: relative;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 280px 1fr;
  background:
    radial-gradient(circle at top left, rgba(14, 116, 144, 0.12), transparent 28%),
    linear-gradient(180deg, #f8fafc 0%, #eef6ff 100%);
}

.sidebar-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.48);
  backdrop-filter: blur(3px);
  z-index: 20;
}

.sidebar {
  position: relative;
  z-index: 30;
  display: flex;
  flex-direction: column;
  gap: 28px;
  padding: 28px 20px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.98) 0%, rgba(15, 23, 42, 0.92) 100%);
  color: #f8fafc;
  border-right: 1px solid rgba(148, 163, 184, 0.18);
  transition: width 0.2s ease, transform 0.2s ease, padding 0.2s ease;
}

.sidebar-collapsed {
  grid-template-columns: 92px 1fr;
}

.sidebar-collapsed .sidebar {
  padding-inline: 14px;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  border-radius: 14px;
  display: grid;
  place-items: center;
  font-weight: 800;
  letter-spacing: 0.06em;
  background: linear-gradient(135deg, #0ea5e9 0%, #38bdf8 100%);
  color: #082f49;
}

.brand-copy h2 {
  margin: 0 0 6px;
  font-size: 1.05rem;
}

.sidebar .muted {
  color: #94a3b8;
}

.nav-links {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #e2e8f0;
  text-decoration: none;
  padding: 12px 14px;
  border-radius: 14px;
  transition: background-color 0.2s ease, transform 0.2s ease;
}

.nav-link:hover,
.nav-link.router-link-active {
  background: rgba(56, 189, 248, 0.16);
  transform: translateX(2px);
}

.nav-badge {
  flex: 0 0 28px;
  width: 28px;
  height: 28px;
  border-radius: 10px;
  display: grid;
  place-items: center;
  background: rgba(148, 163, 184, 0.16);
  font-size: 0.8rem;
  font-weight: 700;
}

.sidebar-footer {
  margin-top: auto;
  padding: 16px;
  border-radius: 16px;
  background: rgba(148, 163, 184, 0.08);
}

.sidebar-footer p {
  margin: 0 0 6px;
}

.main-panel {
  min-width: 0;
  padding: 24px;
}

.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.topbar-main {
  min-width: 0;
  flex: 1;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.toolbar-button {
  padding-inline: 0;
  font-weight: 600;
}

.density-switch {
  display: inline-flex;
  padding: 4px;
  border-radius: 999px;
  background: #e2e8f0;
}

.density-option {
  border: 0;
  background: transparent;
  color: #475569;
  border-radius: 999px;
  padding: 8px 14px;
  font: inherit;
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}

.density-option.active {
  background: #ffffff;
  color: #0f172a;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.08);
}

.topbar-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-card {
  text-align: right;
}

.user-card p {
  margin: 4px 0 0;
}

.role-chip {
  display: inline-block;
  margin-top: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.12);
  color: #1d4ed8;
  font-size: 0.75rem;
  font-weight: 700;
}

@media (max-width: 960px) {
  .app-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: fixed;
    inset: 0 auto 0 0;
    width: min(320px, 86vw);
    transform: translateX(-100%);
    box-shadow: 0 20px 60px rgba(15, 23, 42, 0.24);
  }

  .drawer-open .sidebar {
    transform: translateX(0);
  }
}

@media (max-width: 768px) {
  .main-panel {
    padding: 16px;
  }

  .topbar {
    flex-direction: column;
    align-items: stretch;
    margin-bottom: 16px;
  }

  .toolbar-row,
  .topbar-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .density-switch {
    width: 100%;
  }

  .density-option {
    flex: 1;
  }

  .user-card {
    text-align: left;
  }
}
</style>
