import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { VueQueryPlugin, QueryClient } from '@tanstack/vue-query'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import { setupRouterGuards } from './shared/router-guards'
import './styles.css'

const app = createApp(App)
const pinia = createPinia()
const queryClient = new QueryClient()

app.use(pinia)
app.use(router)
setupRouterGuards(router)
app.use(ElementPlus)
app.use(VueQueryPlugin, { queryClient })

router.afterEach((to) => {
  const title = to.meta.title ? `${to.meta.title} | DocFlow Lite` : 'DocFlow Lite'
  document.title = title
})

app.mount('#app')
