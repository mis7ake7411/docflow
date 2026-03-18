<template>
  <div class="app-layout">
    <aside class="sidebar">
      <div>
        <h2>DocFlow Lite</h2>
        <p class="muted">Vue 3 Admin Console</p>
      </div>

      <nav class="nav-links">
        <RouterLink to="/app">Dashboard</RouterLink>
        <RouterLink to="/app/insights">Insights</RouterLink>
      </nav>
    </aside>

    <main class="main-panel">
      <header class="topbar page-card">
        <div>
          <strong>{{ authStore.user?.username || 'Guest' }}</strong>
          <p class="muted">{{ authStore.user?.email || 'Not logged in' }}</p>
        </div>
        <el-button type="danger" plain @click="handleLogout">登出</el-button>
      </header>

      <section>
        <slot />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

watch(
  () => authStore.isAuthenticated,
  async (isAuthenticated) => {
    if (!isAuthenticated && router.currentRoute.value.path !== '/login') {
      await router.replace('/login')
    }
  },
)

async function handleLogout() {
  await authStore.logout()
  ElMessage.success('已登出')
  await router.replace('/login')
}
</script>

<style scoped>
.app-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px 1fr;
}

.sidebar {
  background: #111827;
  color: #f9fafb;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.sidebar .muted {
  color: #9ca3af;
}

.nav-links {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.nav-links a {
  color: #f9fafb;
  text-decoration: none;
}

.main-panel {
  padding: 24px;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
  gap: 16px;
}

@media (max-width: 960px) {
  .app-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    padding: 20px 24px;
    gap: 16px;
  }

  .nav-links {
    flex-direction: row;
    flex-wrap: wrap;
  }
}

@media (max-width: 768px) {
  .sidebar {
    padding: 16px;
  }

  .main-panel {
    padding: 16px;
  }

  .topbar {
    flex-direction: column;
    align-items: stretch;
    margin-bottom: 16px;
  }
}
</style>
