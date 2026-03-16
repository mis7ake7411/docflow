import { defineStore } from 'pinia'

interface UiState {
  selectedFolderId: number | null
  selectedDocumentId: number | null
}

export const useUiStore = defineStore('ui', {
  state: (): UiState => ({
    selectedFolderId: null,
    selectedDocumentId: null,
  }),
  actions: {
    setSelectedFolderId(folderId: number | null) {
      this.selectedFolderId = folderId
    },
    setSelectedDocumentId(documentId: number | null) {
      this.selectedDocumentId = documentId
    },
  },
})
