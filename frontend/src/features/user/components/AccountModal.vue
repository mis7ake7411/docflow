<template>
  <el-dialog
    :model-value="modelValue"
    title="帳號設定"
    width="480px"
    :close-on-click-modal="true"
    :close-on-press-escape="true"
    class="account-modal"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <!-- 帳號資訊區塊 -->
    <div class="account-profile">
      <div class="account-avatar">{{ avatarInitial }}</div>
      <div class="account-info">
        <strong class="account-username">{{ authStore.user?.username || '訪客' }}</strong>
        <p class="account-email">{{ authStore.user?.email || '尚未登入' }}</p>
        <el-tag size="small" :type="roleTagType">{{ roleLabel }}</el-tag>
      </div>
    </div>

    <el-divider />

    <!-- 修改 Email -->
    <div class="section-title">修改 Email</div>
    <el-form label-position="top" class="account-form">
      <el-form-item label="電子郵件">
        <el-input
          v-model="emailForm.email"
          type="email"
          placeholder="請輸入新的 Email"
          :disabled="emailSubmitting"
        />
      </el-form-item>
      <el-alert
        v-if="emailError"
        :title="emailError"
        type="error"
        show-icon
        :closable="false"
        class="mb-12"
      />
      <el-button
        type="primary"
        plain
        :loading="emailSubmitting"
        @click="handleUpdateEmail"
      >
        儲存 Email
      </el-button>
    </el-form>

    <el-divider />

    <!-- 修改密碼 -->
    <div class="section-title">修改密碼</div>
    <el-form label-position="top" class="account-form">
      <el-form-item label="目前密碼">
        <el-input
          v-model="pwForm.currentPassword"
          type="password"
          placeholder="請輸入目前密碼"
          show-password
          :disabled="pwSubmitting"
        />
      </el-form-item>
      <el-form-item label="新密碼">
        <el-input
          v-model="pwForm.newPassword"
          type="password"
          placeholder="至少 8 碼，需包含英文與數字"
          show-password
          :disabled="pwSubmitting"
        />
      </el-form-item>
      <el-alert
        v-if="pwError"
        :title="pwError"
        type="error"
        show-icon
        :closable="false"
        class="mb-12"
      />
      <el-button
        type="primary"
        plain
        :loading="pwSubmitting"
        @click="handleChangePassword"
      >
        更新密碼
      </el-button>
    </el-form>

    <template #footer>
      <div class="modal-footer">
        <el-button type="danger" plain @click="handleLogout">登出</el-button>
        <el-button @click="emit('update:modelValue', false)">關閉</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { getRoleLabel } from '@/shared/utils/display'
import { updateMyProfile } from '@/features/user/api'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: boolean): void }>()

const authStore = useAuthStore()
const uiStore = useUiStore()
const router = useRouter()

// 頭像首字
const avatarInitial = computed(() =>
  (authStore.user?.username || '訪客').slice(0, 1).toUpperCase(),
)

// 角色標籤
const roleLabel = computed(() => getRoleLabel(authStore.user?.role))
const roleTagType = computed(() => {
  switch (authStore.user?.role) {
    case 'ADMIN':
      return 'danger'
    case 'MANAGER':
      return 'warning'
    default:
      return 'info'
  }
})

// --- 修改 Email ---
const emailForm = reactive({ email: authStore.user?.email || '' })
const emailSubmitting = ref(false)
const emailError = ref('')

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      emailForm.email = authStore.user?.email || ''
      emailError.value = ''
      pwForm.currentPassword = ''
      pwForm.newPassword = ''
      pwError.value = ''
    }
  },
)

async function handleUpdateEmail() {
  if (!emailForm.email.trim()) {
    emailError.value = '請輸入 Email'
    return
  }
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(emailForm.email.trim())) {
    emailError.value = 'Email 格式不正確'
    return
  }
  emailSubmitting.value = true
  emailError.value = ''
  try {
    const updated = await updateMyProfile({ email: emailForm.email.trim() })
    if (authStore.user) {
      authStore.setUserSummary({ ...authStore.user, email: updated.email })
    }
    ElMessage.success('Email 已更新')
  } catch {
    emailError.value = '更新失敗，請稍後再試'
  } finally {
    emailSubmitting.value = false
  }
}

// --- 修改密碼 ---
const pwForm = reactive({ currentPassword: '', newPassword: '' })
const pwSubmitting = ref(false)
const pwError = ref('')

function isPasswordValid(value: string) {
  return /^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(value)
}

async function handleChangePassword() {
  if (!pwForm.currentPassword || !pwForm.newPassword) {
    pwError.value = '請輸入目前密碼與新密碼'
    return
  }
  if (!isPasswordValid(pwForm.newPassword)) {
    pwError.value = '密碼至少 8 碼，且需包含英文與數字'
    return
  }
  pwSubmitting.value = true
  pwError.value = ''
  try {
    await authStore.changePassword({
      currentPassword: pwForm.currentPassword,
      newPassword: pwForm.newPassword,
    })
    pwForm.currentPassword = ''
    pwForm.newPassword = ''
    ElMessage.success('密碼已更新')
  } catch {
    pwError.value = '更新失敗，請確認目前密碼是否正確'
  } finally {
    pwSubmitting.value = false
  }
}

// --- 登出 ---
async function handleLogout() {
  await authStore.logout()
  uiStore.closeSidebarDrawer()
  emit('update:modelValue', false)
  ElMessage.success('已登出')
  await router.replace('/login')
}
</script>

<style scoped>
.account-profile {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0 4px;
}

.account-avatar {
  width: 56px;
  height: 56px;
  border-radius: 999px;
  display: grid;
  place-items: center;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 1.5rem;
  font-weight: 700;
  flex-shrink: 0;
}

.account-username {
  display: block;
  font-size: 1.1rem;
  margin-bottom: 4px;
}

.account-email {
  margin: 0 0 6px;
  color: #748092;
  font-size: 0.9rem;
}

.section-title {
  font-weight: 600;
  color: #122033;
  margin-bottom: 12px;
}

.account-form {
  margin-bottom: 4px;
}

.mb-12 {
  margin-bottom: 12px;
}

.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>

