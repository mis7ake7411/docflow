<template>
  <div class="folder-panel">
    <div class="section-header">
      <div>
        <h3>資料夾列表</h3>
      </div>
      <el-button v-if="!isManager" type="primary" @click="openCreateDialog">新增資料夾</el-button>
    </div>

    <div class="folder-content">
      <el-skeleton v-if="isLoading" :rows="6" animated />
      <el-alert v-else-if="error" title="資料夾樹載入失敗" type="error" show-icon :closable="false" />
      <el-empty v-else-if="!treeData.length" description="目前沒有自己的資料夾" />

      <el-tree
        v-else
        :data="treeData"
        node-key="id"
        :props="treeProps"
        highlight-current
        default-expand-all
        draggable
        :allow-drag="allowDrag"
        :allow-drop="allowDrop"
        @node-click="handleNodeClick"
        @node-drop="handleNodeDrop"
      >
        <template #default="{ data }">
          <div class="tree-node">
            <span class="node-name">
              <span v-if="showDragHandle(data)" class="drag-handle">⋮⋮</span>
              {{ data.name }}
            </span>
            <span v-if="!isManager" class="tree-actions">
              <el-tooltip v-if="!canEditFolder(data)" :content="PERMISSION_MESSAGES.folderHint">
                <span>
                  <el-button text size="small" disabled @click.stop>編輯</el-button>
                </span>
              </el-tooltip>
              <el-button v-else text size="small" @click.stop="openEditDialog(data)">編輯</el-button>

              <el-tooltip v-if="!canEditFolder(data)" :content="PERMISSION_MESSAGES.folderHint">
                <span>
                  <el-button text size="small" type="danger" disabled>刪除</el-button>
                </span>
              </el-tooltip>
              <el-popconfirm v-else title="確定刪除這個資料夾？" @confirm="handleDelete(data.id)">
                <template #reference>
                  <el-button text size="small" type="danger" @click.stop>刪除</el-button>
                </template>
              </el-popconfirm>
            </span>
          </div>
        </template>
      </el-tree>
    </div>

    <div class="panel-footer">
      <button type="button" class="footer-link" @click="handleAllDocuments">回到全部文件</button>
    </div>

    <FolderFormDialog v-model="createDialogVisible" :folder="null" :tree-data="treeData" />
    <FolderFormDialog v-model="editDialogVisible" :folder="editingFolder" :tree-data="treeData" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useMutation, useQuery, useQueryClient } from '@tanstack/vue-query'
import { ElMessage } from 'element-plus'
import { deleteFolder, getFolderTree, reorderFolders, type FolderTreeNode } from '@/features/folder/api'
import FolderFormDialog from '@/features/folder/components/FolderFormDialog.vue'
import { useUiStore } from '@/stores/ui'
import { useAuthStore } from '@/stores/auth'
import { canModifyResource, isAdminOrManager, PERMISSION_MESSAGES } from '@/shared/utils/permission'
import { isAxiosError } from 'axios'

type DropType = 'prev' | 'inner' | 'next'

type TreeNodeLike = {
  data: FolderTreeNode
  parent: {
    level: number
    data?: FolderTreeNode
  }
}

const uiStore = useUiStore()
const authStore = useAuthStore()
const queryClient = useQueryClient()

const createDialogVisible = ref(false)
const editDialogVisible = ref(false)
const editingFolder = ref<FolderTreeNode | null>(null)
const currentUser = computed(() => authStore.user)
const currentUserId = computed(() => currentUser.value?.id ?? null)

const { data, isLoading, error, isSuccess } = useQuery({
  queryKey: computed(() => ['folders', 'tree', currentUserId.value]),
  queryFn: getFolderTree,
})

const deleteMutation = useMutation({
  mutationFn: deleteFolder,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['folders', 'tree'] })
    ElMessage.success('資料夾已刪除')
  },
  onError: (error) => {
    if (isAxiosError(error) && error.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.folderForbidden)
    }
  },
})

const reorderMutation = useMutation({
  mutationFn: reorderFolders,
  onSuccess: async () => {
    await queryClient.invalidateQueries({ queryKey: ['folders', 'tree'] })
    ElMessage.success('資料夾排序已更新')
  },
  onError: async (error) => {
    await queryClient.invalidateQueries({ queryKey: ['folders', 'tree'] })
    if (isAxiosError(error) && error.response?.status === 403) {
      ElMessage.error(PERMISSION_MESSAGES.folderForbidden)
      return
    }
    ElMessage.error('資料夾排序更新失敗')
  },
})

const treeData = computed(() => data.value ?? [])
const treeProps = {
  children: 'children',
  label: 'name',
}
const selectedFolderId = computed(() => uiStore.selectedFolderId)

// const totalCount = computed(() => countNodes(treeData.value))
const isManager = computed(() => authStore.userRole === 'MANAGER')

watch(isSuccess, (ready) => {
  uiStore.setFolderTreeReady(ready)
}, { immediate: true })

watch(currentUserId, (nextUserId) => {
  if (nextUserId == null) {
    return
  }

  if (uiStore.folderContextUserId !== nextUserId) {
    uiStore.syncFolderContextForUser(nextUserId)
  }
}, { immediate: true })

watch(
  [() => uiStore.folderTreeReady, treeData, selectedFolderId],
  ([ready, folders, folderId]) => {
    if (!ready || folderId == null || !folders) {
      return
    }

    if (!findNodeById(folders, folderId)) {
      uiStore.clearSelectedFolderId()
    }
  },
  { immediate: true },
)

function handleNodeClick(node: FolderTreeNode) {
  uiStore.setSelectedFolderId(node.id)
}

function handleAllDocuments() {
  uiStore.clearSelectedFolderId()
}

function openCreateDialog() {
  createDialogVisible.value = true
}

function openEditDialog(folder: FolderTreeNode) {
  editingFolder.value = folder
  editDialogVisible.value = true
}

function canEditFolder(folder: FolderTreeNode) {
  return canModifyResource(folder.createdBy, currentUser.value)
}

function canReorderSiblingGroup(parentId: number | null) {
  if (isAdminOrManager(currentUser.value)) {
    return true
  }
  const siblings = getSiblingGroup(parentId)
  return siblings.length > 0 && siblings.every((folder) => canEditFolder(folder))
}

function showDragHandle(folder: FolderTreeNode) {
  return canReorderSiblingGroup(folder.parentId)
}

function allowDrag(node: TreeNodeLike) {
  return canReorderSiblingGroup(node.data.parentId)
}

function allowDrop(draggingNode: TreeNodeLike, dropNode: TreeNodeLike, type: DropType) {
  return type !== 'inner'
    && draggingNode.data.parentId === dropNode.data.parentId
    && canReorderSiblingGroup(draggingNode.data.parentId)
}

async function handleNodeDrop(
  draggingNode: TreeNodeLike,
  dropNode: TreeNodeLike,
  dropType: DropType,
) {
  if (dropType === 'inner') {
    return
  }

  const parentId = dropNode.data.parentId
  const siblings = getSiblingGroup(parentId)
  const currentOrder = siblings.map((folder) => folder.id)
  const orderedFolderIds = buildReorderedIds(
    currentOrder,
    draggingNode.data.id,
    dropNode.data.id,
    dropType,
  )

  if (hasSameOrder(currentOrder, orderedFolderIds)) {
    return
  }

  await reorderMutation.mutateAsync({
    parentId,
    orderedFolderIds,
  })
}

async function handleDelete(folderId: number) {
  await deleteMutation.mutateAsync(folderId)
}

function getSiblingGroup(parentId: number | null): FolderTreeNode[] {
  if (parentId == null) {
    return treeData.value
  }
  const parent = findNodeById(treeData.value, parentId)
  return parent?.children ?? []
}

function buildReorderedIds(
  currentOrder: number[],
  draggedId: number,
  targetId: number,
  dropType: Exclude<DropType, 'inner'>,
): number[] {
  if (draggedId === targetId) {
    return currentOrder
  }

  const remainingIds = currentOrder.filter((id) => id !== draggedId)
  const targetIndex = remainingIds.indexOf(targetId)

  if (targetIndex === -1) {
    return currentOrder
  }

  const insertIndex = dropType === 'prev' ? targetIndex : targetIndex + 1
  const reorderedIds = [...remainingIds]
  reorderedIds.splice(insertIndex, 0, draggedId)
  return reorderedIds
}

function hasSameOrder(currentOrder: number[], nextOrder: number[]) {
  return currentOrder.length === nextOrder.length
    && currentOrder.every((id, index) => id === nextOrder[index])
}

// function countNodes(nodes: FolderTreeNode[]): number {
//   return nodes.reduce((sum, node) => sum + 1 + countNodes(node.children ?? []), 0)
// }

function findNodeById(nodes: FolderTreeNode[], targetId: number): FolderTreeNode | null {
  for (const node of nodes) {
    if (node.id === targetId) {
      return node
    }
    const childMatch = findNodeById(node.children ?? [], targetId)
    if (childMatch) {
      return childMatch
    }
  }
  return null
}
</script>

<style scoped>
.folder-panel {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 24px 24px 18px;
  border-bottom: 1px solid #edf2f7;
}

.section-header h3,
.section-header p {
  margin: 0;
}

.section-header p {
  margin-top: 8px;
}

.folder-content {
  flex: 1;
  padding: 20px 24px;
}

.tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  min-width: 0;
  gap: 12px;
}

.node-name {
  flex: 1;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drag-handle {
  color: #94a3b8;
  cursor: grab;
  letter-spacing: -1px;
}

.tree-actions {
  margin-left: auto;
  flex: 0 0 112px;
  display: inline-flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  white-space: nowrap;
}

.panel-footer {
  padding: 0 24px 20px;
}

.footer-link {
  border: 0;
  background: transparent;
  color: #26b3bc;
  cursor: pointer;
  padding: 0;
  font: inherit;
}

@media (max-width: 768px) {
  .section-header {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
  }

  .folder-content,
  .panel-footer {
    padding-inline: 16px;
  }

  .tree-actions {
    flex-basis: auto;
    justify-content: flex-end;
  }
}

</style>
