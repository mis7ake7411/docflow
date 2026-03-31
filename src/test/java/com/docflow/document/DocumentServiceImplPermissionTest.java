package com.docflow.document;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.exception.ForbiddenException;
import com.docflow.common.security.DocflowUserPrincipal;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.UpdateDocumentRequest;
import com.docflow.document.entity.Document;
import com.docflow.document.entity.DocumentStatus;
import com.docflow.document.repository.DocumentRepository;
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
import org.springframework.core.io.ByteArrayResource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private DocumentServiceImpl documentService;

    @BeforeEach
    void setUp() {
        documentRepository = Mockito.mock(DocumentRepository.class);
        folderRepository = Mockito.mock(FolderRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        localFileStorageService = Mockito.mock(LocalFileStorageService.class);
        documentCacheService = Mockito.mock(DocumentCacheService.class);
        activityLogService = Mockito.mock(ActivityLogService.class);

        documentService = new DocumentServiceImpl(
                documentRepository,
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
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.update(1L, buildUpdateRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限操作此文件");
    }

    @Test
    void userCannotUploadOthersDocument() {
        setCurrentUser(2L, UserRole.USER);
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.upload(1L, buildMultipartFile()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限操作此文件");
    }

    @Test
    void userCannotDeleteOthersDocument() {
        setCurrentUser(2L, UserRole.USER);
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.delete(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限操作此文件");
    }

    @Test
    void userCannotUploadOwnerlessDocument() {
        setCurrentUser(2L, UserRole.USER);
        Document document = buildDocument(1L, null);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.upload(1L, buildMultipartFile()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限操作此文件");
    }

    @Test
    void userCannotDeleteOwnerlessDocument() {
        setCurrentUser(2L, UserRole.USER);
        Document document = buildDocument(1L, null);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.delete(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限操作此文件");
    }

    @Test
    void userCanUpdateOwnDocument() {
        setCurrentUser(1L, UserRole.USER);
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(Mockito.anyLong()))
                .thenReturn(Optional.of(buildFolder()));
        Mockito.when(documentRepository.save(Mockito.any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.update(1L, buildUpdateRequest());

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void userCanUploadOwnDocument() {
        setCurrentUser(1L, UserRole.USER);
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));
        Mockito.when(localFileStorageService.store(Mockito.any()))
                .thenReturn(buildStoredFileResult());
        Mockito.when(documentRepository.save(Mockito.any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.upload(1L, buildMultipartFile());

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void adminCanUploadOwnerlessDocument() {
        setCurrentUser(2L, UserRole.ADMIN);
        Document document = buildDocument(1L, null);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));
        Mockito.when(localFileStorageService.store(Mockito.any()))
                .thenReturn(buildStoredFileResult());
        Mockito.when(documentRepository.save(Mockito.any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.upload(1L, buildMultipartFile());

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void userCanDeleteOwnDocument() {
        setCurrentUser(1L, UserRole.USER);
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));
        Mockito.when(documentRepository.save(Mockito.any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        documentService.delete(1L);

        assertThat(document.isDeletedFlag()).isTrue();
    }

    @Test
    void managerCanDeleteOwnerlessDocument() {
        setCurrentUser(2L, UserRole.MANAGER);
        Document document = buildDocument(1L, null);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));
        Mockito.when(documentRepository.save(Mockito.any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        documentService.delete(1L);

        assertThat(document.isDeletedFlag()).isTrue();
    }

    @Test
    void adminCanDeleteOthersDocument() {
        setCurrentUser(2L, UserRole.ADMIN);
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));
        Mockito.when(documentRepository.save(Mockito.any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        documentService.delete(1L);

        assertThat(document.isDeletedFlag()).isTrue();
    }

    @Test
    void managerCanUpdateOthersDocument() {
        setCurrentUser(2L, UserRole.MANAGER);
        Document document = buildDocument(1L, 1L);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(Mockito.anyLong()))
                .thenReturn(Optional.of(buildFolder()));
        Mockito.when(documentRepository.save(Mockito.any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.update(1L, buildUpdateRequest());

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void adminCanUpdateDocumentWithoutCreator() {
        setCurrentUser(2L, UserRole.ADMIN);
        Document document = buildDocument(1L, null);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));
        Mockito.when(folderRepository.findByIdAndDeletedFlagFalse(Mockito.anyLong()))
                .thenReturn(Optional.of(buildFolder()));
        Mockito.when(documentRepository.save(Mockito.any(Document.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = documentService.update(1L, buildUpdateRequest());

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void userCannotUpdateDocumentWithoutCreator() {
        setCurrentUser(2L, UserRole.USER);
        Document document = buildDocument(1L, null);
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.update(1L, buildUpdateRequest()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("無權限操作此文件");
    }

    @Test
    void getByIdShouldRecordRecentViewWhenDocumentExists() {
        setCurrentUser(2L, UserRole.USER);
        DocumentResponse cachedResponse = buildDocumentResponse(1L);
        Mockito.when(documentCacheService.getDocumentDetail(1L))
                .thenReturn(Optional.of(cachedResponse));

        DocumentResponse response = documentService.getById(1L);

        assertThat(response).isSameAs(cachedResponse);
        verify(documentCacheService).recordDocumentView(2L, cachedResponse);
    }

    @Test
    void getByIdShouldNotRecordRecentViewWhenDocumentDoesNotExist() {
        setCurrentUser(2L, UserRole.USER);
        Mockito.when(documentCacheService.getDocumentDetail(1L))
                .thenReturn(Optional.empty());
        Mockito.when(documentRepository.findByIdAndDeletedFlagFalse(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getById(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Document not found");

        verify(documentCacheService, never()).recordDocumentView(Mockito.anyLong(), Mockito.any());
    }

    @Test
    void downloadShouldRecordRecentViewOnlyOnce() {
        setCurrentUser(2L, UserRole.USER);
        DocumentResponse cachedResponse = buildDocumentResponse(1L, "doc.txt", "stored-doc.txt");
        Mockito.when(documentCacheService.getDocumentDetail(1L))
                .thenReturn(Optional.of(cachedResponse));
        Mockito.when(localFileStorageService.loadAsResource("stored-doc.txt"))
                .thenReturn(new ByteArrayResource("content".getBytes()));

        documentService.download(1L);

        verify(documentCacheService, Mockito.times(1)).recordDocumentView(2L, cachedResponse);
    }

    private void setCurrentUser(Long userId, UserRole role) {
        User user = buildUser(userId, role);
        DocflowUserPrincipal principal = new DocflowUserPrincipal(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
        Mockito.lenient().when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));
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

    private MultipartFile buildMultipartFile() {
        return new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes()
        );
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
}
