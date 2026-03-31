package com.docflow.document;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.exception.ForbiddenException;
import com.docflow.common.security.DocflowUserPrincipal;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.DocumentShareItemResponse;
import com.docflow.document.dto.ShareDocumentRequest;
import com.docflow.document.dto.UpdateDocumentRequest;
import com.docflow.document.entity.Document;
import com.docflow.document.entity.DocumentAccessLevel;
import com.docflow.document.entity.DocumentShare;
import com.docflow.document.entity.DocumentSharePermission;
import com.docflow.document.entity.DocumentStatus;
import com.docflow.document.repository.DocumentRepository;
import com.docflow.document.repository.DocumentShareRepository;
import com.docflow.document.service.DocumentCacheService;
import com.docflow.document.service.DocumentServiceImpl;
import com.docflow.document.storage.LocalFileStorageService;
import com.docflow.document.storage.StoredFileResult;
import com.docflow.folder.entity.Folder;
import com.docflow.folder.repository.FolderRepository;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplPermissionTest {

    private DocumentRepository documentRepository;
    private FolderRepository folderRepository;
    private UserRepository userRepository;
    private LocalFileStorageService localFileStorageService;
    private DocumentCacheService documentCacheService;
    private ActivityLogService activityLogService;
    private DocumentShareRepository documentShareRepository;

    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentRepository = Mockito.mock(DocumentRepository.class);
        folderRepository = Mockito.mock(FolderRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        localFileStorageService = Mockito.mock(LocalFileStorageService.class);
        documentCacheService = Mockito.mock(DocumentCacheService.class);
        activityLogService = Mockito.mock(ActivityLogService.class);
        documentShareRepository = Mockito.mock(DocumentShareRepository.class);

        documentService = new DocumentServiceImpl(
                documentRepository,
                documentShareRepository,
                folderRepository,
                userRepository,
                localFileStorageService,
                documentCacheService,
                activityLogService
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void userCannotUpdateOthersDocument() {
        setCurrentUser(2L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));

        assertThatThrownBy(() -> documentService.update(1L, buildUpdateRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限編輯此文件");
    }

    @Test
    void userCannotUploadOthersDocument() {
        setCurrentUser(2L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));

        assertThatThrownBy(() -> documentService.upload(1L, buildMultipartFile()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限編輯此文件");
    }

    @Test
    void userCannotDeleteOthersDocument() {
        setCurrentUser(2L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));

        assertThatThrownBy(() -> documentService.delete(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限操作此文件");
    }

    @Test
    void userCanUpdateOwnDocument() {
        setCurrentUser(1L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(anyLong())).thenReturn(Optional.of(buildFolder()));
        Mockito.when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.update(1L, buildUpdateRequest());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAccessLevel()).isEqualTo(DocumentAccessLevel.OWNER.name());
    }

    @Test
    void userCanUploadOwnDocument() {
        setCurrentUser(1L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(localFileStorageService.store(any())).thenReturn(buildStoredFileResult());
        Mockito.when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.upload(1L, buildMultipartFile());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAccessLevel()).isEqualTo(DocumentAccessLevel.OWNER.name());
    }

    @Test
    void adminCanDeleteOthersDocument() {
        setCurrentUser(2L, UserRole.ADMIN);
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(document));
        Mockito.when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        documentService.delete(1L);

        assertThat(document.isDeletedFlag()).isTrue();
    }

    @Test
    void getByIdShouldRecordRecentViewWhenDocumentExists() {
        setCurrentUser(2L, UserRole.USER);
        Document document = buildDocument(1L, 1L);
        DocumentResponse cachedResponse = buildDocumentResponse(1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(document));
        Mockito.when(documentCacheService.getDocumentDetail(1L)).thenReturn(Optional.of(cachedResponse));
        Mockito.when(documentShareRepository.findByDocumentIdAndSharedWithUserId(1L, 2L))
                .thenReturn(Optional.of(buildShare(1L, 1L, 2L, DocumentSharePermission.VIEW)));

        DocumentResponse response = documentService.getById(1L);

        assertThat(response).isSameAs(cachedResponse);
        assertThat(response.getAccessLevel()).isEqualTo(DocumentAccessLevel.VIEW.name());
        verify(documentCacheService).recordDocumentView(2L, cachedResponse);
    }

    @Test
    void getByIdShouldNotRecordRecentViewWhenDocumentDoesNotExist() {
        setCurrentUser(2L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getById(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Document not found");

        verify(documentCacheService, never()).recordDocumentView(anyLong(), any());
    }

    @Test
    void downloadShouldRecordRecentViewOnlyOnce() {
        setCurrentUser(2L, UserRole.USER);
        Document document = buildDocument(1L, 1L);
        DocumentResponse cachedResponse = buildDocumentResponse(1L, "doc.txt", "stored-doc.txt");
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(document));
        Mockito.when(documentCacheService.getDocumentDetail(1L)).thenReturn(Optional.of(cachedResponse));
        Mockito.when(documentShareRepository.findByDocumentIdAndSharedWithUserId(1L, 2L))
                .thenReturn(Optional.of(buildShare(1L, 1L, 2L, DocumentSharePermission.VIEW)));
        Mockito.when(localFileStorageService.loadAsResource("stored-doc.txt"))
                .thenReturn(new ByteArrayResource("content".getBytes()));

        documentService.download(1L);

        verify(documentCacheService).recordDocumentView(2L, cachedResponse);
    }

    @Test
    void sharedViewUserCanGetDocumentDetail() {
        setCurrentUser(2L, UserRole.USER);
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(document));
        Mockito.when(documentCacheService.getDocumentDetail(1L)).thenReturn(Optional.empty());
        Mockito.when(documentShareRepository.findByDocumentIdAndSharedWithUserId(1L, 2L))
                .thenReturn(Optional.of(buildShare(1L, 1L, 2L, DocumentSharePermission.VIEW)));

        DocumentResponse response = documentService.getById(1L);

        assertThat(response.getAccessLevel()).isEqualTo(DocumentAccessLevel.VIEW.name());
        assertThat(response.getSharedBy()).isEqualTo("user1");
        verify(documentCacheService).cacheDocumentDetail(eq(1L), any(DocumentResponse.class));
    }

    @Test
    void sharedViewUserCannotUpdateDocument() {
        setCurrentUser(2L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(documentShareRepository.findByDocumentIdAndSharedWithUserId(1L, 2L))
                .thenReturn(Optional.of(buildShare(1L, 1L, 2L, DocumentSharePermission.VIEW)));

        assertThatThrownBy(() -> documentService.update(1L, buildUpdateRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限編輯此文件");
    }

    @Test
    void sharedEditUserCanUpdateDocument() {
        setCurrentUser(2L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(documentShareRepository.findByDocumentIdAndSharedWithUserId(1L, 2L))
                .thenReturn(Optional.of(buildShare(1L, 1L, 2L, DocumentSharePermission.EDIT)));
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(anyLong())).thenReturn(Optional.of(buildFolder()));
        Mockito.when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.update(1L, buildUpdateRequest());

        assertThat(response.getAccessLevel()).isEqualTo(DocumentAccessLevel.EDIT.name());
    }

    @Test
    void nonOwnerWithoutShareCannotGetDocumentDetail() {
        setCurrentUser(2L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(documentShareRepository.findByDocumentIdAndSharedWithUserId(1L, 2L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getById(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限查看此文件");
    }

    @Test
    void ownerCanShareDocumentWithAnotherUser() {
        setCurrentUser(1L, UserRole.USER);
        ShareDocumentRequest request = new ShareDocumentRequest();
        request.setSharedWithUserId(2L);
        request.setPermission("VIEW");
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, UserRole.USER)));
        Mockito.when(documentShareRepository.findByDocumentIdAndSharedWithUserId(1L, 2L)).thenReturn(Optional.empty());
        Mockito.when(documentShareRepository.save(any(DocumentShare.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentShareItemResponse response = documentService.createShare(1L, request);

        assertThat(response.getPermission()).isEqualTo(DocumentSharePermission.VIEW.name());
        assertThat(response.getUsername()).isEqualTo("user2");
        verify(documentCacheService).evictDocumentDetail(1L);
        assertLoggedShareDetail("SHARE_CREATE", detail -> {
            assertThat(detail).containsEntry("sharedWithUserId", 2L);
            assertThat(detail).containsEntry("sharedWithUsername", "user2");
            assertThat(detail).containsEntry("permission", DocumentSharePermission.VIEW.name());
        });
    }

    @Test
    void ownerCanUpdateSharePermission() {
        setCurrentUser(1L, UserRole.USER);
        ShareDocumentRequest request = new ShareDocumentRequest();
        request.setPermission("EDIT");
        DocumentShare share = buildShare(7L, 1L, 2L, DocumentSharePermission.VIEW);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(documentShareRepository.findByIdAndDocumentId(7L, 1L)).thenReturn(Optional.of(share));
        Mockito.when(documentShareRepository.save(any(DocumentShare.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DocumentShareItemResponse response = documentService.updateShare(1L, 7L, request);

        assertThat(response.getPermission()).isEqualTo(DocumentSharePermission.EDIT.name());
        verify(documentCacheService).evictDocumentDetail(1L);
        assertLoggedShareDetail("SHARE_UPDATE", detail -> {
            assertThat(detail).containsEntry("sharedWithUserId", 2L);
            assertThat(detail).containsEntry("sharedWithUsername", "user2");
            assertThat(detail).containsEntry("previousPermission", DocumentSharePermission.VIEW.name());
            assertThat(detail).containsEntry("permission", DocumentSharePermission.EDIT.name());
        });
    }

    @Test
    void ownerCanDeleteShare() {
        setCurrentUser(1L, UserRole.USER);
        DocumentShare share = buildShare(7L, 1L, 2L, DocumentSharePermission.VIEW);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(documentShareRepository.findByIdAndDocumentId(7L, 1L)).thenReturn(Optional.of(share));

        documentService.deleteShare(1L, 7L);

        verify(documentShareRepository).delete(share);
        verify(documentCacheService).evictDocumentDetail(1L);
        assertLoggedShareDetail("SHARE_DELETE", detail -> {
            assertThat(detail).containsEntry("sharedWithUserId", 2L);
            assertThat(detail).containsEntry("sharedWithUsername", "user2");
        });
    }

    @Test
    void duplicateShareShouldBeRejected() {
        setCurrentUser(1L, UserRole.USER);
        ShareDocumentRequest request = new ShareDocumentRequest();
        request.setSharedWithUserId(2L);
        request.setPermission("VIEW");
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(userRepository.findById(2L)).thenReturn(Optional.of(buildUser(2L, UserRole.USER)));
        Mockito.when(documentShareRepository.findByDocumentIdAndSharedWithUserId(1L, 2L))
                .thenReturn(Optional.of(buildShare(9L, 1L, 2L, DocumentSharePermission.VIEW)));

        assertThatThrownBy(() -> documentService.createShare(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("文件已分享給此使用者");
    }

    @Test
    void sharedWithMeReturnsOnlySharedDocuments() {
        setCurrentUser(2L, UserRole.USER);
        DocumentShare share = buildShare(1L, 5L, 2L, DocumentSharePermission.EDIT);
        Mockito.when(documentShareRepository.findAllBySharedWithUserIdAndDocumentDeletedFlagFalseOrderByCreatedAtDesc(eq(2L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(share)));

        var response = documentService.getSharedWithMe(0, 10);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getAccessLevel()).isEqualTo(DocumentAccessLevel.EDIT.name());
        assertThat(response.getItems().get(0).getSharedBy()).isEqualTo("user1");
    }

    @Test
    void ownerCanListShares() {
        setCurrentUser(1L, UserRole.USER);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L)).thenReturn(Optional.of(buildDocument(1L, 1L)));
        Mockito.when(documentShareRepository.findAllByDocumentIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(buildShare(1L, 1L, 2L, DocumentSharePermission.VIEW)));

        List<DocumentShareItemResponse> response = documentService.getShares(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getUsername()).isEqualTo("user2");
    }

    private void setCurrentUser(Long userId, UserRole role) {
        User user = buildUser(userId, role);
        DocflowUserPrincipal principal = new DocflowUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        Mockito.lenient().when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    private Document buildDocument(Long documentId, Long createdById) {
        User creator = createdById == null ? null : buildUser(createdById, UserRole.USER);
        return Document.builder()
                .id(documentId)
                .title("Doc")
                .description("Desc")
                .version(1)
                .status(DocumentStatus.ACTIVE)
                .createdBy(creator)
                .deletedFlag(false)
                .build();
    }

    private Folder buildFolder() {
        return Folder.builder()
                .id(10L)
                .name("Folder")
                .sortOrder(0)
                .createdBy(buildUser(1L, UserRole.USER))
                .deletedFlag(false)
                .build();
    }

    private UpdateDocumentRequest buildUpdateRequest() {
        UpdateDocumentRequest request = new UpdateDocumentRequest();
        request.setFolderId(10L);
        request.setTitle("New Title");
        request.setDescription("New Description");
        request.setStatus("ACTIVE");
        return request;
    }

    private DocumentResponse buildDocumentResponse(Long documentId) {
        return buildDocumentResponse(documentId, null, null);
    }

    private DocumentResponse buildDocumentResponse(Long documentId, String fileName, String storedFileName) {
        return DocumentResponse.builder()
                .id(documentId)
                .title("Doc")
                .description("Desc")
                .fileName(fileName)
                .storedFileName(storedFileName)
                .version(1)
                .status(DocumentStatus.ACTIVE.name())
                .createdBy(1L)
                .build();
    }

    private DocumentShare buildShare(Long shareId, Long documentId, Long sharedUserId, DocumentSharePermission permission) {
        return DocumentShare.builder()
                .id(shareId)
                .document(buildDocument(documentId, 1L))
                .sharedWith(buildUser(sharedUserId, UserRole.USER))
                .permission(permission)
                .createdBy(buildUser(1L, UserRole.USER))
                .createdAt(LocalDateTime.now())
                .build();
    }

    private MultipartFile buildMultipartFile() {
        return new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
    }

    private StoredFileResult buildStoredFileResult() {
        return StoredFileResult.builder()
                .originalFileName("test.txt")
                .storedFileName("stored-test.txt")
                .contentType("text/plain")
                .fileSize(7L)
                .build();
    }

    private User buildUser(Long id, UserRole role) {
        return User.builder()
                .id(id)
                .username("user" + id)
                .email("user" + id + "@example.com")
                .passwordHash("hash")
                .role(role)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @SuppressWarnings("unchecked")
    private void assertLoggedShareDetail(String action, java.util.function.Consumer<java.util.Map<String, Object>> assertions) {
        ArgumentCaptor<java.util.Map<String, Object>> detailCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        verify(activityLogService).log(eq(1L), eq("DOCUMENT"), eq(1L), eq(action), detailCaptor.capture());
        assertions.accept(detailCaptor.getValue());
    }
}
