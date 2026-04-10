import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin, QueryClient } from '@tanstack/vue-query'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { registerAuthBridge } from './shared/api/auth-bridge'
import { setupRouterGuards } from './shared/router-guards'
import { useAuthStore } from './stores/auth'
import './styles.css'

const SESSION_EXPIRED_REASON_KEY = 'docflow.sessionExpiredReason'
const DEFAULT_SESSION_EXPIRED_MESSAGE = '登入狀態已過期，請重新登入。'

const app = createApp(App)
const pinia = createPinia()
const queryClient = new QueryClient()

function resolveSessionExpiredReason(reason?: string) {
  return reason?.trim() || DEFAULT_SESSION_EXPIRED_MESSAGE
}

function buildLoginPath() {
  const currentPath = `${window.location.pathname}${window.location.search}`

  if (currentPath === '/login' || currentPath.startsWith('/login?')) {
    return '/login'
  }

  if (currentPath === '/register' || currentPath.startsWith('/register?')) {
    return '/login'
  }

  return `/login?redirect=${encodeURIComponent(currentPath)}`
}

async function redirectToLogin() {
  const targetPath = buildLoginPath()

  if (router.currentRoute.value.fullPath === targetPath) {
    return
  }

  try {
    await router.replace(targetPath)
  } catch {
    window.location.replace(targetPath)
  }
}

app.use(pinia)

const authStore = useAuthStore(pinia)
registerAuthBridge({
  getAccessToken: () => authStore.accessToken,
  refreshAccessToken: () => authStore.refreshAccessToken(),
  onLogout: (reason?: string) => {
    const message = resolveSessionExpiredReason(reason)

    authStore.setSessionExpired(message)
    authStore.initialized = true
    window.sessionStorage.setItem(SESSION_EXPIRED_REASON_KEY, message)

    void redirectToLogin()
  },
})

app.use(router)
setupRouterGuards(router)
app.use(ElementPlus)
app.use(VueQueryPlugin, { queryClient })

router.afterEach((to) => {
  const title = to.meta.title ? `${to.meta.title} | DocFlow Lite` : 'DocFlow Lite'
  document.title = title
})

app.mount('#app')
