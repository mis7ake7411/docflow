<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="900px"
    @close="emit('update:modelValue', false)"
  >
    <div class="share-dialog">
      <section class="share-section">
        <div class="section-header">
          <div>
            <h3>分享給使用者</h3>
            <p class="muted">搜尋站內使用者並設定檢視或編輯權限。</p>
          </div>
        </div>

        <div class="search-row">
          <el-input
            v-model="searchKeyword"
            placeholder="輸入帳號或 Email"
            clearable
            @keyup.enter="handleSearch"
          />
          <el-button type="primary" @click="handleSearch">搜尋</el-button>
        </div>

        <el-skeleton v-if="isSearching" :rows="4" animated />
        <el-alert
          v-else-if="searchSubmittedKeyword && searchResultsError"
          title="使用者搜尋失敗"
          type="error"
          show-icon
          :closable="false"
        />
        <el-empty
          v-else-if="searchSubmittedKeyword && !searchResults.length"
          description="找不到符合條件的使用者"
        />

        <el-table v-else-if="searchSubmittedKeyword" :data="searchResults" size="small" stripe>
          <el-table-column prop="username" label="帳號" min-width="140" />
          <el-table-column prop="email" label="Email" min-width="220" />
          <el-table-column label="角色" width="120">
            <template #default="scope">
              {{ getRoleLabel(scope.row.role) }}
            </template>
          </el-table-column>
          <el-table-column label="權限" width="160">
            <template #default="scope">
              <el-select v-model="grantPermissions[scope.row.id]" size="small" style="width: 100%">
                <el-option label="可檢視" value="VIEW" />
                <el-option label="可編輯" value="EDIT" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="scope">
              <el-button
                size="small"
                type="primary"
                :loading="addingShareUserId === scope.row.id"
                :disabled="scope.row.id === currentUserId"
                @click="handleAddShare(scope.row.id)"
              >
                新增分享
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="share-section">
        <div class="section-header">
          <div>
            <h3>目前分享名單</h3>
            <p class="muted">可更新權限或取消既有分享。</p>
          </div>
        </div>

        <el-skeleton v-if="isLoadingShares" :rows="4" animated />
        <el-alert
          v-else-if="sharesError"
          title="分享名單載入失敗"
          type="error"
          show-icon
          :closable="false"
        />
        <el-empty v-else-if="!shares.length" description="尚未分享給任何使用者" />

        <el-table v-else :data="shares" size="small" stripe>
          <el-table-column prop="username" label="使用者" min-width="160" />
          <el-table-column prop="email" label="Email" min-width="220" />
          <el-table-column label="權限" width="160">
            <template #default="scope">
              <el-select v-model="scope.row.permission" size="small" style="width: 100%">
                <el-option label="可檢視" value="VIEW" />
                <el-option label="可編輯" value="EDIT" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="分享者" min-width="140">
            <template #default="scope">
              {{ scope.row.sharedBy || '未知' }}
            </template>
          </el-table-column>
          <el-table-column label="建立時間" min-width="180">
            <template #default="scope">
              {{ formatDate(scope.row.createdAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="scope">
              <el-button
                size="small"
                :loading="updatingShareId === scope.row.id"
                @click="handleUpdateShare(scope.row.id, scope.row.permission)"
              >
                更新
              </el-button>
              <el-popconfirm title="確定取消這筆分享？" @confirm="handleDeleteShare(scope.row.id)">
                <template #reference>
                  <el-button size="small" type="danger">移除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </section>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import {
  addDocumentShare,
  deleteDocumentShare,
  getDocumentShares,
  updateDocumentShare,
  type DocumentAccessLevel,
} from '@/features/document/api'
import { getShareCandidates } from '@/features/user/api'
import { useAuthStore } from '@/stores/auth'
import { getRoleLabel } from '@/shared/utils/display'
import { PERMISSION_MESSAGES } from '@/shared/utils/permission'
import { isAxiosError } from 'axios'

const props = defineProps<{
  modelValue: boolean
  documentId: number
  documentTitle: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const authStore = useAuthStore()
const queryClient = useQueryClient()
const searchKeyword = ref('')
const searchSubmittedKeyword = ref('')
const grantPermissions = ref<Record<number, Extract<DocumentAccessLevel, 'VIEW' | 'EDIT'>>>({})
const addingShareUserId = ref<number | null>(null)
const updatingShareId = ref<number | null>(null)

const dialogTitle = computed(() =>
  props.documentTitle ? `分享文件：${props.documentTitle}` : '分享文件',
)
const currentUserId = computed(() => authStore.user?.id ?? null)

const sharesQuery = useQuery({
  queryKey: computed(() => ['documents', 'shares', props.documentId]),
  queryFn: () => getDocumentShares(props.documentId),
  enabled: computed(() => props.modelValue),
})

const searchQuery = useQuery({
  queryKey: computed(() => ['users', 'share-candidates', searchSubmittedKeyword.value]),
  queryFn: () => getShareCandidates(searchSubmittedKeyword.value || undefined),
  enabled: computed(() => props.modelValue && Boolean(searchSubmittedKeyword.value)),
})

const shares = computed(() => sharesQuery.data.value ?? [])
const isLoadingShares = computed(() => sharesQuery.isLoading.value)
const sharesError = computed(() => sharesQuery.error.value)
const searchResults = computed(() => searchQuery.data.value ?? [])
const isSearching = computed(() => searchQuery.isLoading.value)
const searchResultsError = computed(() => searchQuery.error.value)

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      searchKeyword.value = ''
      searchSubmittedKeyword.value = ''
      grantPermissions.value = {}
    }
  },
)

watch(
  searchResults,
  (users) => {
    for (const user of users) {
      if (!grantPermissions.value[user.id]) {
        grantPermissions.value[user.id] = 'VIEW'
      }
    }
  },
  { immediate: true },
)

const addShareMutation = useMutation({
  mutationFn: ({ userId, permission }: { userId: number; permission: Extract<DocumentAccessLevel, 'VIEW' | 'EDIT'> }) =>
    addDocumentShare(props.documentId, { sharedWithUserId: userId, permission }),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['documents'] })
    await queryClient.invalidateQueries({ queryKey: ['documents', 'shares', props.documentId] })
    ElMessage.success('分享已新增')
  },
  onError: (error) => {
    if (isAxiosError(error) && error.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.documentShareForbidden)
      return
    }
    ElMessage.error('新增分享失敗')
  },
})

const updateShareMutation = useMutation({
  mutationFn: ({ shareId, permission }: { shareId: number; permission: Extract<DocumentAccessLevel, 'VIEW' | 'EDIT'> }) =>
    updateDocumentShare(props.documentId, shareId, { sharedWithUserId: 0, permission }),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['documents'] })
    await queryClient.invalidateQueries({ queryKey: ['documents', 'shares', props.documentId] })
    ElMessage.success('分享權限已更新')
  },
  onError: (error) => {
    if (isAxiosError(error) && error.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.documentShareForbidden)
      return
    }
    ElMessage.error('更新分享失敗')
  },
})

const deleteShareMutation = useMutation({
  mutationFn: (shareId: number) => deleteDocumentShare(props.documentId, shareId),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['documents'] })
    await queryClient.invalidateQueries({ queryKey: ['documents', 'shares', props.documentId] })
    ElMessage.success('分享已移除')
  },
  onError: (error) => {
    if (isAxiosError(error) && error.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.documentShareForbidden)
      return
    }
    ElMessage.error('移除分享失敗')
  },
})

function handleSearch() {
  searchSubmittedKeyword.value = searchKeyword.value.trim()
}

async function handleAddShare(userId: number) {
  addingShareUserId.value = userId
  try {
    await addShareMutation.mutateAsync({
      userId,
      permission: grantPermissions.value[userId] ?? 'VIEW',
    })
  } finally {
    addingShareUserId.value = null
  }
}

async function handleUpdateShare(shareId: number, permission: Extract<DocumentAccessLevel, 'VIEW' | 'EDIT'>) {
  updatingShareId.value = shareId
  try {
    await updateShareMutation.mutateAsync({ shareId, permission })
  } finally {
    updatingShareId.value = null
  }
}

async function handleDeleteShare(shareId: number) {
  await deleteShareMutation.mutateAsync(shareId)
}

function formatDate(value: string) {
  return new Date(value).toLocaleString('zh-TW')
}
</script>

<style scoped>
.share-dialog {
  display: grid;
  gap: 24px;
}

.share-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-header h3,
.section-header p {
  margin: 0;
}

.section-header p {
  margin-top: 8px;
}

.search-row {
  display: flex;
  gap: 12px;
}

@media (max-width: 768px) {
  .search-row {
    flex-direction: column;
  }
}
</style>
