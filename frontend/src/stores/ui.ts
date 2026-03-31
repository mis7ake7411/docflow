import { defineStore } from 'pinia'

type ContentDensity = 'comfortable' | 'compact'

interface UiState {
  selectedFolderId: number | null
  selectedDocumentId: number | null
  folderTreeReady: boolean
  folderContextUserId: number | null
  sidebarCollapsed: boolean
  sidebarDrawerOpen: boolean
  contentDensity: ContentDensity
}

const SIDEBAR_COLLAPSED_KEY = 'docflow.ui.sidebarCollapsed'
const CONTENT_DENSITY_KEY = 'docflow.ui.contentDensity'

function readBoolean(key: string, fallback: boolean) {
  const value = localStorage.getItem(key)
  if (value === null) {
    return fallback
  }

  return value === 'true'
}

function readDensity(): ContentDensity {
  const value = localStorage.getItem(CONTENT_DENSITY_KEY)
  return value === 'compact' ? 'compact' : 'comfortable'
}

export const useUiStore = defineStore('ui', {
  state: (): UiState => ({
    selectedFolderId: null,
    selectedDocumentId: null,
    folderTreeReady: false,
    folderContextUserId: null,
    sidebarCollapsed: readBoolean(SIDEBAR_COLLAPSED_KEY, false),
    sidebarDrawerOpen: false,
    contentDensity: readDensity(),
  }),
  actions: {
    setSelectedFolderId(folderId: number | null) {
      this.selectedFolderId = folderId
    },
    clearSelectedFolderId() {
      this.selectedFolderId = null
    },
    setFolderTreeReady(ready: boolean) {
      this.folderTreeReady = ready
    },
    resetFolderContext() {
      this.selectedFolderId = null
      this.folderTreeReady = false
    },
    setFolderContextUserId(userId: number | null) {
      this.folderContextUserId = userId
    },
    syncFolderContextForUser(userId: number | null) {
      if (this.folderContextUserId !== userId) {
        this.resetFolderContext()
        this.folderContextUserId = userId
      }
    },
    setSelectedDocumentId(documentId: number | null) {
      this.selectedDocumentId = documentId
    },
    toggleSidebarCollapsed() {
      this.sidebarCollapsed = !this.sidebarCollapsed
      localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(this.sidebarCollapsed))
    },
    setSidebarCollapsed(collapsed: boolean) {
      this.sidebarCollapsed = collapsed
      localStorage.setItem(SIDEBAR_COLLAPSED_KEY, String(collapsed))
    },
    openSidebarDrawer() {
      this.sidebarDrawerOpen = true
    },
    closeSidebarDrawer() {
      this.sidebarDrawerOpen = false
    },
    toggleSidebarDrawer() {
      this.sidebarDrawerOpen = !this.sidebarDrawerOpen
    },
    setContentDensity(density: ContentDensity) {
      this.contentDensity = density
      localStorage.setItem(CONTENT_DENSITY_KEY, density)
    },
  },
})
