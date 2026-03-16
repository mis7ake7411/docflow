<template>
  <el-dialog :model-value="modelValue" :title="isEdit ? '編輯資料夾' : '新增資料夾'" width="520px" @close="emit('update:modelValue', false)">
    <el-form label-position="top">
      <el-form-item label="Name">
        <el-input v-model="form.name" placeholder="輸入資料夾名稱" />
      </el-form-item>

      <el-form-item label="Parent Folder">
        <el-select v-model="form.parentId" clearable placeholder="選擇父資料夾" style="width: 100%">
          <el-option :value="null" label="無" />
          <el-option
            v-for="option in parentOptions"
            :key="option.id"
            :label="option.name"
            :value="option.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="Sort Order">
        <el-input-number v-model="form.sortOrder" :min="0" style="width: 100%" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">儲存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import { createFolder, updateFolder, type FolderPayload, type FolderTreeNode } from '@/features/folder/api'

const props = defineProps<{
  modelValue: boolean
  folder?: FolderTreeNode | null
  treeData: FolderTreeNode[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const queryClient = useQueryClient()
const submitting = ref(false)
const isEdit = computed(() => Boolean(props.folder))

const form = reactive<FolderPayload>({
  name: '',
  parentId: null,
  sortOrder: 0,
})

watch(
  () => props.folder,
  (folder) => {
    if (folder) {
      form.name = folder.name
      form.parentId = folder.parentId
      form.sortOrder = folder.sortOrder
    } else {
      form.name = ''
      form.parentId = null
      form.sortOrder = 0
    }
  },
  { immediate: true },
)

const parentOptions = computed(() => flattenFolders(props.treeData, props.folder?.id ?? null))

const createMutation = useMutation({
  mutationFn: createFolder,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['folders', 'tree'] })
    ElMessage.success('資料夾建立成功')
    emit('update:modelValue', false)
  },
})

const updateMutation = useMutation({
  mutationFn: ({ id, payload }: { id: number; payload: FolderPayload }) => updateFolder(id, payload),
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['folders', 'tree'] })
    ElMessage.success('資料夾更新成功')
    emit('update:modelValue', false)
  },
})

async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.error('請輸入資料夾名稱')
    return
  }

  submitting.value = true
  try {
    const payload: FolderPayload = {
      name: form.name,
      parentId: form.parentId,
      sortOrder: form.sortOrder,
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
</script>
