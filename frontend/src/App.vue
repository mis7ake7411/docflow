<template>
  <ElConfigProvider :locale="zhTw">
    <AuthBootstrap>
      <router-view />
    </AuthBootstrap>
  </ElConfigProvider>
</template>

<script setup lang="ts">
import { ElConfigProvider } from 'element-plus'
import zhTw from 'element-plus/es/locale/lang/zh-tw'
import AuthBootstrap from '@/features/auth/components/AuthBootstrap.vue'
</script>
