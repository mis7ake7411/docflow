<template>
  <el-dialog
    :model-value="modelValue"
    :title="isEdit ? '編輯自己的資料夾' : '新增自己的資料夾'"
    width="520px"
    @close="emit('update:modelValue', false)"
  >
    <el-form label-position="top">
      <el-form-item label="名稱">
        <el-input v-model="form.name" placeholder="請輸入自己的資料夾名稱" />
      </el-form-item>

      <el-form-item label="上層資料夾">
        <el-select v-model="form.parentId" clearable placeholder="選擇上層資料夾（可留空）" style="width: 100%">
          <el-option :value="null" label="根資料夾" />
          <el-option
            v-for="option in parentOptions"
            :key="option.id"
            :label="option.name"
            :value="option.id"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="isManager" @click="handleSubmit">儲存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import { createFolder, updateFolder, type FolderPayload, type FolderTreeNode } from '@/features/folder/api'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'
import { PERMISSION_MESSAGES } from '@/shared/utils/permission'
import { isAxiosError } from 'axios'

const props = defineProps<{
  modelValue: boolean
  folder?: FolderTreeNode | null
  treeData: FolderTreeNode[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const queryClient = useQueryClient()
const authStore = useAuthStore()
const uiStore = useUiStore()
const submitting = ref(false)
const isEdit = computed(() => Boolean(props.folder))
const isManager = computed(() => authStore.userRole === 'MANAGER')

const form = reactive<FolderPayload>({
  name: '',
  parentId: null,
})

watch(
  () => props.folder,
  (folder) => {
    if (folder) {
      form.name = folder.name
      form.parentId = folder.parentId
    } else {
      form.name = ''
      form.parentId = null
    }
  },
  { immediate: true },
)

const parentOptions = computed(() => flattenFolders(props.treeData, props.folder?.id ?? null))

watch(
  [() => props.treeData, () => uiStore.folderTreeReady],
  ([treeData, ready]) => {
    if (!ready || form.parentId == null) {
      return
    }

    if (!findFolderById(treeData, form.parentId)) {
      form.parentId = null
    }
  },
  { immediate: true },
)

const createMutation = useMutation({
  mutationFn: createFolder,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['folders', 'tree'] })
    ElMessage.success('資料夾已建立')
    emit('update:modelValue', false)
  },
})

const updateMutation = useMutation({
  mutationFn: ({ id, payload }: { id: number; payload: FolderPayload }) => updateFolder(id, payload),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['folders', 'tree'] })
    ElMessage.success('資料夾已更新')
    emit('update:modelValue', false)
  },
  onError: (error) => {
    if (isAxiosError(error) && error.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.folderForbidden)
    }
  },
})

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.error('請輸入自己的資料夾名稱')
    return
  }

  submitting.value = true
  try {
    const payload: FolderPayload = {
      name: form.name.trim(),
      parentId: form.parentId,
    }

    if (props.folder) {
      await updateMutation.mutateAsync({ id: props.folder.id, payload })
    } else {
      await createMutation.mutateAsync(payload)
    }
  } finally {
    submitting.value = false
  }
}

function flattenFolders(nodes: FolderTreeNode[], excludeId: number | null): Array<{ id: number; name: string }> {
  const result: Array<{ id: number; name: string }> = []

  const walk = (items: FolderTreeNode[], prefix = '') => {
    for (const item of items) {
      if (excludeId !== null && item.id === excludeId) {
        continue
      }
      result.push({ id: item.id, name: prefix ? `${prefix} / ${item.name}` : item.name })
      if (item.children?.length) {
        walk(item.children, prefix ? `${prefix} / ${item.name}` : item.name)
      }
    }
  }

  walk(nodes)
  return result
}

function findFolderById(nodes: FolderTreeNode[], targetId: number): boolean {
  for (const node of nodes) {
    if (node.id === targetId) {
      return true
    }
    if (node.children?.length && findFolderById(node.children, targetId)) {
      return true
    }
  }
  return false
}
</script>
