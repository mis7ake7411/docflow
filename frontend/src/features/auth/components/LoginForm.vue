<template>
  <el-form ref="formRef" :model="form" label-position="top" @submit.prevent="handleSubmit">
    <el-form-item label="Username">
      <el-input v-model="form.username" placeholder="請輸入帳號" />
    </el-form-item>

    <el-form-item label="Password">
      <el-input v-model="form.password" type="password" placeholder="請輸入密碼" show-password />
    </el-form-item>

    <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" class="mb-16" />

    <el-button type="primary" :loading="submitting" @click="handleSubmit">登入</el-button>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const formRef = ref()
const submitting = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  password: '',
})

async function handleSubmit() {
  if (!form.username || !form.password) {
    errorMessage.value = '請輸入帳號與密碼'
    return
  }

  submitting.value = true
  errorMessage.value = ''

  try {
    await authStore.login({
      username: form.username,
      password: form.password,
    })
    ElMessage.success('登入成功')
    await router.push('/app')
  } catch (error) {
    errorMessage.value = '登入失敗，請檢查帳號密碼或後端服務狀態'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}
</style>
