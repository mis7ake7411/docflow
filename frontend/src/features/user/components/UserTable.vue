<template>
  <div class="user-table">
    <div class="table-toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜尋帳號或 Email" class="search-input" @keyup.enter="handleSearch" />
        <el-button @click="handleSearch">搜尋</el-button>
      </div>
      <el-button type="primary" @click="openCreateDialog">新增使用者</el-button>
    </div>

    <el-skeleton v-if="isLoading" :rows="8" animated />
    <el-alert v-else-if="error" title="使用者清單載入失敗" type="error" show-icon :closable="false" />
    <el-empty v-else-if="!items.length" description="目前沒有使用者" />

    <el-table v-else :data="items" stripe>
      <el-table-column prop="username" label="帳號" min-width="140" />
      <el-table-column prop="email" label="Email" min-width="200" />
      <el-table-column label="角色" width="140">
        <template #default="scope">
          {{ getRoleLabel(scope.row.role) }}
        </template>
      </el-table-column>
      <el-table-column label="狀態" width="120">
        <template #default="scope">
          {{ getStatusLabel(scope.row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="建立時間" min-width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button text @click="openEditDialog(scope.row)">編輯</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!isLoading && !error && totalElements" class="pagination">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="pageSizes"
        :total="totalElements"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <el-dialog :model-value="createDialogVisible" title="新增使用者" width="480px" @close="closeCreateDialog">
      <el-form label-position="top">
        <el-form-item label="帳號">
          <el-input v-model="createForm.username" placeholder="請輸入帳號" />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="createForm.email" placeholder="請輸入 Email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="createForm.role" style="width: 100%">
            <el-option label="一般使用者" value="USER" />
            <el-option label="主管" value="MANAGER" />
            <el-option label="管理員" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="狀態">
          <el-select v-model="createForm.status" style="width: 100%">
            <el-option label="啟用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeCreateDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">建立</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="editDialogVisible" title="編輯使用者" width="420px" @close="closeEditDialog">
      <el-form label-position="top">
        <el-form-item label="帳號">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="Email">
          <el-input v-model="editForm.email" placeholder="請輸入 Email" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option label="一般使用者" value="USER" />
            <el-option label="主管" value="MANAGER" />
            <el-option label="管理員" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="狀態">
          <el-select v-model="editForm.status" style="width: 100%">
            <el-option label="啟用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="closeEditDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleUpdate">更新</el-button>
      </template>
    </el-dialog>

    <el-dialog :model-value="tempPasswordDialogVisible" title="建立完成" width="420px" @close="closeTempPassword">
      <p class="temp-hint">請將以下臨時密碼提供給使用者，首次登入需更新密碼。</p>
      <div class="temp-password">{{ tempPassword }}</div>
      <template #footer>
        <el-button type="primary" @click="closeTempPassword">我已記下</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import {
  createUser,
  getUsers,
  updateUser,
  type CreateUserRequest,
  type UpdateUserRequest,
  type UserListItem,
} from '@/features/user/api'
import { getRoleLabel, getStatusLabel } from '@/shared/utils/display'

const queryClient = useQueryClient()

const currentPage = ref(1)
const pageSize = ref(10)
const pageSizes = [10, 20, 50]
const keyword = ref('')

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const tempPasswordDialogVisible = ref(false)
const tempPassword = ref('')
const editingUserId = ref<number | null>(null)
const submitting = ref(false)

const createForm = reactive<CreateUserRequest>({
  username: '',
  email: '',
  role: 'USER',
  status: 'ACTIVE',
})

const editForm = reactive<{ username: string; email: string; role: string; status: string }>({
  username: '',
  email: '',
  role: 'USER',
  status: 'ACTIVE',
})

const { data, isLoading, error } = useQuery({
  queryKey: computed(() => ['users', currentPage.value, pageSize.value, keyword.value]),
  queryFn: () => getUsers(currentPage.value - 1, pageSize.value, keyword.value.trim() || undefined),
})

const createMutation = useMutation({
  mutationFn: createUser,
  onSuccess: async (response) => {
    await queryClient.invalidateQueries({ queryKey: ['users'] })
    tempPassword.value = response.tempPassword
    tempPasswordDialogVisible.value = true
    ElMessage.success('使用者已建立')
    closeCreateDialog()
  },
})

const updateMutation = useMutation({
  mutationFn: ({ id, payload }: { id: number; payload: UpdateUserRequest }) => updateUser(id, payload),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['users'] })
    ElMessage.success('使用者已更新')
    closeEditDialog()
  },
})

const items = computed(() => data.value?.items ?? [])
const totalElements = computed(() => data.value?.totalElements ?? 0)

function handleSearch() {
  currentPage.value = 1
  void queryClient.invalidateQueries({ queryKey: ['users'] })
}

function handlePageChange(page: number) {
  currentPage.value = page
}

function handleSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
}

function openCreateDialog() {
  createForm.username = ''
  createForm.email = ''
  createForm.role = 'USER'
  createForm.status = 'ACTIVE'
  createDialogVisible.value = true
}

function closeCreateDialog() {
  createDialogVisible.value = false
}

function openEditDialog(user: UserListItem) {
  editingUserId.value = user.id
  editForm.username = user.username
  editForm.email = user.email
  editForm.role = user.role
  editForm.status = user.status
  editDialogVisible.value = true
}

function closeEditDialog() {
  editDialogVisible.value = false
  editingUserId.value = null
}

function closeTempPassword() {
  tempPasswordDialogVisible.value = false
  tempPassword.value = ''
}

async function handleCreate() {
  if (!createForm.username.trim() || !createForm.email.trim()) {
    ElMessage.error('請填寫帳號與 Email')
    return
  }

  submitting.value = true
  try {
    await createMutation.mutateAsync({
      username: createForm.username.trim(),
      email: createForm.email.trim(),
      role: createForm.role,
      status: createForm.status,
    })
  } finally {
    submitting.value = false
  }
}

async function handleUpdate() {
  if (!editingUserId.value) {
    return
  }

  if (!editForm.email.trim()) {
    ElMessage.error('請填寫 Email')
    return
  }

  submitting.value = true
  try {
    await updateMutation.mutateAsync({
      id: editingUserId.value,
      payload: {
        email: editForm.email.trim(),
        role: editForm.role,
        status: editForm.status,
      },
    })
  } finally {
    submitting.value = false
  }
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-TW')
}
</script>

<style scoped>
.user-table {
  padding: 20px 28px 24px;
}

.table-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.search-input {
  width: 240px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.temp-hint {
  margin: 0 0 12px;
  color: #6b7280;
}

.temp-password {
  padding: 12px 16px;
  border-radius: 12px;
  background: #f3f4f6;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-align: center;
}

@media (max-width: 768px) {
  .user-table {
    padding: 16px;
  }

  .table-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    width: 100%;
  }

  .search-input {
    width: 100%;
  }

  .pagination {
    justify-content: center;
  }
}
</style>
