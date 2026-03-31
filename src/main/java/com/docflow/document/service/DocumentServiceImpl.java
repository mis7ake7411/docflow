package com.docflow.document.service;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.exception.ForbiddenException;
import com.docflow.common.response.PagedResponse;
import com.docflow.common.security.SecurityUtils;
import com.docflow.document.dto.CreateDocumentRequest;
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
import com.docflow.document.storage.LocalFileStorageService;
import com.docflow.document.storage.StoredFileResult;
import com.docflow.folder.entity.Folder;
import com.docflow.folder.repository.FolderRepository;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;

/**
 * {@link DocumentService} 的預設實作，負責文件資料、檔案儲存與快取同步。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentShareRepository documentShareRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final LocalFileStorageService localFileStorageService;
    private final DocumentCacheService documentCacheService;
    private final ActivityLogService activityLogService;

    /**
     * 建立文件基本資料並清除相關快取。
     *
     * @param request 建立資料
     * @return 建立後的文件資訊
     */
    @Override
    @Transactional
    public DocumentResponse create(CreateDocumentRequest request) {
        log.info("Creating document: title={}, folderId={}, status={}",
                request.getTitle(), request.getFolderId(), request.getStatus());
        User currentUser = getCurrentUser();
        Folder folder = resolveFolder(request.getFolderId());

        Document document = documentRepository.save(Document.builder()
                .folder(folder)
                .title(request.getTitle())
                .description(request.getDescription())
                .version(1)
                .status(parseStatus(request.getStatus()))
                .createdBy(currentUser)
                .deletedFlag(false)
                .build());

        documentCacheService.evictDocumentDetail(document.getId());
        activityLogService.log(currentUser.getId(), "DOCUMENT", document.getId(), "CREATE", java.util.Map.of(
                "title", document.getTitle(),
                "status", document.getStatus().name()
        ));
        return toAccessibleResponse(document, currentUser, null);
    }

    /**
     * 上傳文件內容、更新版本並清除明細快取。
     *
     * @param id 文件編號
     * @param file 上傳檔案
     * @return 更新後的文件資訊
     */
    @Override
    @Transactional
    public DocumentResponse upload(Long id, MultipartFile file) {
        log.info("Uploading document file: documentId={}, originalFilename={}",
                id, file != null ? file.getOriginalFilename() : null);
        User currentUser = getCurrentUser();
        Document document = getActiveDocument(id);
        AccessContext accessContext = requireEditAccess(document, currentUser);
        StoredFileResult storedFile = localFileStorageService.store(file);

        document.setFileName(storedFile.getOriginalFileName());
        document.setStoredFileName(storedFile.getStoredFileName());
        document.setContentType(storedFile.getContentType());
        document.setFileSize(storedFile.getFileSize());
        document.setVersion(document.getVersion() + 1);

        Document saved = documentRepository.save(document);
        log.info("Document file uploaded successfully: documentId={}, version={}", saved.getId(), saved.getVersion());
        DocumentResponse response = toResponse(saved);
        documentCacheService.evictDocumentDetail(id);
        activityLogService.log(currentUser.getId(), "DOCUMENT", saved.getId(), "UPLOAD", java.util.Map.of(
                "fileName", saved.getFileName(),
                "storedFileName", saved.getStoredFileName(),
                "version", saved.getVersion()
        ));
        return toAccessibleResponse(saved, currentUser, accessContext);
    }

    /**
     * 取得所有未刪除文件。
     *
     * @return 文件列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAll() {
        User currentUser = getCurrentUser();
        log.debug("Loading currentUser : {}",currentUser);
        return documentRepository.findAllByDeletedFlagFalseAndCreatedBy_Id(currentUser.getId(), PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .map(document -> toAccessibleResponse(document, currentUser, AccessContext.owner()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DocumentResponse> getPaged(int page, int size, Long folderId) {
      log.debug("Loading paged document list: page={}, size={}, folderId={}", page, size, folderId);
      User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Document> results = folderId == null
                ? documentRepository.findAllByDeletedFlagFalseAndCreatedBy_Id(currentUser.getId(), pageable)
                : documentRepository.findAllByDeletedFlagFalseAndCreatedBy_IdAndFolder_Id(currentUser.getId(), folderId, pageable);

        return PagedResponse.<DocumentResponse>builder()
                .items(results.stream()
                        .map(document -> toAccessibleResponse(document, currentUser, AccessContext.owner()))
                        .toList())
                .page(results.getNumber())
                .size(results.getSize())
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DocumentResponse> getSharedWithMe(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<DocumentShare> results = documentShareRepository
                .findAllBySharedWithUserIdAndDocumentDeletedFlagFalseOrderByCreatedAtDesc(currentUser.getId(), pageable);

        return PagedResponse.<DocumentResponse>builder()
                .items(results.stream().map(this::toSharedDocumentResponse).toList())
                .page(results.getNumber())
                .size(results.getSize())
                .totalElements(results.getTotalElements())
                .totalPages(results.getTotalPages())
                .build();
    }

    /**
     * 先從快取取得文件明細；快取未命中時改由資料庫查詢並回寫快取。
     *
     * @param id 文件編號
     * @return 文件資訊
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentResponse getById(Long id) {
        log.debug("Loading document detail: documentId={}", id);
        User currentUser = getCurrentUser();
        Document document = getActiveDocument(id);
        AccessContext accessContext = requireViewAccess(document, currentUser);
        DocumentResponse response = documentCacheService.getDocumentDetail(id)
                .orElseGet(() -> {
                    log.debug("Document cache miss: documentId={}", id);
                    DocumentResponse loadedResponse = toResponse(document);
                    documentCacheService.cacheDocumentDetail(id, loadedResponse);
                    return loadedResponse;
                });
        applyAccessContext(response, accessContext);
        documentCacheService.recordDocumentView(currentUser.getId(), response);
        return response;
    }

    /**
     * 更新文件基本資料、提升版本並清除明細快取。
     *
     * @param id 文件編號
     * @param request 更新資料
     * @return 更新後的文件資訊
     */
    @Override
    @Transactional
    public DocumentResponse update(Long id, UpdateDocumentRequest request) {
        log.info("Updating document: documentId={}, title={}, folderId={}, status={}",
                id, request.getTitle(), request.getFolderId(), request.getStatus());
        User currentUser = getCurrentUser();
        Document document = getActiveDocument(id);
        AccessContext accessContext = requireEditAccess(document, currentUser);
        Folder folder = resolveFolder(request.getFolderId());

        document.setFolder(folder);
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setStatus(parseStatus(request.getStatus()));
        document.setVersion(document.getVersion() + 1);

        Document saved = documentRepository.save(document);
        log.info("Document updated successfully: documentId={}, version={}", saved.getId(), saved.getVersion());
        documentCacheService.evictDocumentDetail(id);
        activityLogService.log(currentUser.getId(), "DOCUMENT", saved.getId(), "UPDATE", java.util.Map.of(
                "title", saved.getTitle(),
                "status", saved.getStatus().name(),
                "version", saved.getVersion()
        ));
        return toAccessibleResponse(saved, currentUser, accessContext);
    }

    @Override
    @Transactional
    public DocumentShareItemResponse createShare(Long documentId, ShareDocumentRequest request) {
        User currentUser = getCurrentUser();
        Document document = getActiveDocument(documentId);
        requireShareManagement(document, currentUser);

        User sharedWith = resolveShareTarget(request.getSharedWithUserId(), currentUser.getId());
        documentShareRepository.findByDocumentIdAndSharedWithUserId(documentId, sharedWith.getId())
                .ifPresent(existing -> {
                    throw new BadRequestException("文件已分享給此使用者");
                });

        DocumentShare saved = documentShareRepository.save(DocumentShare.builder()
                .document(document)
                .sharedWith(sharedWith)
                .permission(parseSharePermission(request.getPermission()))
                .createdBy(currentUser)
                .build());

        documentCacheService.evictDocumentDetail(documentId);
        activityLogService.log(currentUser.getId(), "DOCUMENT", documentId, "SHARE_CREATE", java.util.Map.of(
                "sharedWithUserId", sharedWith.getId(),
                "sharedWithUsername", sharedWith.getUsername(),
                "permission", saved.getPermission().name()
        ));
        return toShareItemResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentShareItemResponse> getShares(Long documentId) {
        User currentUser = getCurrentUser();
        Document document = getActiveDocument(documentId);
        requireShareManagement(document, currentUser);

        return documentShareRepository.findAllByDocumentIdOrderByCreatedAtAsc(documentId).stream()
                .map(this::toShareItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public DocumentShareItemResponse updateShare(Long documentId, Long shareId, ShareDocumentRequest request) {
        User currentUser = getCurrentUser();
        Document document = getActiveDocument(documentId);
        requireShareManagement(document, currentUser);

        DocumentShare share = documentShareRepository.findByIdAndDocumentId(shareId, documentId)
                .orElseThrow(() -> new BadRequestException("Document share not found"));
        DocumentSharePermission previousPermission = share.getPermission();
        DocumentSharePermission newPermission = parseSharePermission(request.getPermission());
        share.setPermission(newPermission);

        DocumentShare saved = documentShareRepository.save(share);
        documentCacheService.evictDocumentDetail(documentId);
        activityLogService.log(currentUser.getId(), "DOCUMENT", documentId, "SHARE_UPDATE", java.util.Map.of(
                "sharedWithUserId", share.getSharedWith().getId(),
                "sharedWithUsername", share.getSharedWith().getUsername(),
                "previousPermission", previousPermission.name(),
                "permission", saved.getPermission().name()
        ));
        return toShareItemResponse(saved);
    }

    @Override
    @Transactional
    public void deleteShare(Long documentId, Long shareId) {
        User currentUser = getCurrentUser();
        Document document = getActiveDocument(documentId);
        requireShareManagement(document, currentUser);

        DocumentShare share = documentShareRepository.findByIdAndDocumentId(shareId, documentId)
                .orElseThrow(() -> new BadRequestException("Document share not found"));
        documentShareRepository.delete(share);
        documentCacheService.evictDocumentDetail(documentId);
        activityLogService.log(currentUser.getId(), "DOCUMENT", documentId, "SHARE_DELETE", java.util.Map.of(
                "sharedWithUserId", share.getSharedWith().getId(),
                "sharedWithUsername", share.getSharedWith().getUsername()
        ));
    }

    /**
     * 軟刪除指定文件並清除明細快取。
     *
     * @param id 文件編號
     */
    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Deleting document: documentId={}", id);
        User currentUser = getCurrentUser();
        Document document = getActiveDocument(id);
        requireDeleteAccess(document, currentUser);

        document.setDeletedFlag(true);
        documentRepository.save(document);
        log.info("Document soft-deleted successfully: documentId={}", document.getId());
        documentCacheService.evictDocumentDetail(id);
        activityLogService.log(currentUser.getId(), "DOCUMENT", document.getId(), "DELETE", java.util.Map.of(
                "title", document.getTitle()
        ));
    }

    /**
     * 下載文件內容，並同步記錄瀏覽與活動紀錄。
     *
     * @param id 文件編號
     * @return 可供下載的檔案資源
     */
    @Override
    @Transactional
    public Resource download(Long id) {
        log.info("Downloading document: documentId={}", id);
        DocumentResponse response = getById(id);
        if (response.getStoredFileName() == null || response.getStoredFileName().isBlank()) {
            log.warn("Document download rejected because no file is stored: documentId={}", id);
            throw new BadRequestException("Document file has not been uploaded");
        }
        activityLogService.log(getCurrentUser().getId(), "DOCUMENT", response.getId(), "DOWNLOAD", java.util.Map.of(
                "fileName", response.getFileName(),
                "version", response.getVersion()
        ));
        return localFileStorageService.loadAsResource(response.getStoredFileName());
    }

    /**
     * 取得目前登入使用者。
     *
     * @return 目前登入使用者
     */
    private User getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.debug("Resolving current user for document operation: userId={}", userId);
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Current user not found"));
    }

    /**
     * 取得未被刪除的文件，找不到時拋出例外。
     *
     * @param id 文件編號
     * @return 文件實體
     */
    private Document getActiveDocument(Long id) {
        return documentRepository.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new BadRequestException("Document not found"));
    }

    /**
     * 解析文件所屬資料夾；未提供資料夾時回傳 {@code null}。
     *
     * @param folderId 資料夾編號
     * @return 資料夾實體或 {@code null}
     */
    private Folder resolveFolder(Long folderId) {
        if (folderId == null) {
            return null;
        }
        log.debug("Resolving document folder: folderId={}", folderId);
        return folderRepository.findByIdAndDeletedFlagFalse(folderId)
                .orElseThrow(() -> new BadRequestException("Folder not found"));
    }

    private User resolveShareTarget(Long sharedWithUserId, Long currentUserId) {
        if (sharedWithUserId == null) {
            throw new BadRequestException("sharedWithUserId is required");
        }
        User sharedWith = userRepository.findById(sharedWithUserId)
                .orElseThrow(() -> new BadRequestException("User not found"));
        if (sharedWith.getStatus() != UserStatus.ACTIVE) {
            throw new BadRequestException("User is not active");
        }
        if (sharedWith.getId().equals(currentUserId)) {
            throw new BadRequestException("Cannot share a document with yourself");
        }
        return sharedWith;
    }

    private AccessContext requireViewAccess(Document document, User currentUser) {
        if (isPrivileged(currentUser)) {
            return AccessContext.admin();
        }
        if (isOwner(currentUser, document)) {
            return AccessContext.owner();
        }
        return documentShareRepository.findByDocumentIdAndSharedWithUserId(document.getId(), currentUser.getId())
                .map(share -> AccessContext.shared(share.getPermission(), share.getCreatedBy()))
                .orElseThrow(() -> new ForbiddenException("無權限查看此文件"));
    }

    private AccessContext requireEditAccess(Document document, User currentUser) {
        if (isPrivileged(currentUser)) {
            return AccessContext.admin();
        }
        if (isOwner(currentUser, document)) {
            return AccessContext.owner();
        }
        return documentShareRepository.findByDocumentIdAndSharedWithUserId(document.getId(), currentUser.getId())
                .filter(share -> share.getPermission() == DocumentSharePermission.EDIT)
                .map(share -> AccessContext.shared(DocumentSharePermission.EDIT, share.getCreatedBy()))
                .orElseThrow(() -> new ForbiddenException("無權限編輯此文件"));
    }

    private void requireDeleteAccess(Document document, User currentUser) {
        if (isPrivileged(currentUser) || isOwner(currentUser, document)) {
            return;
        }
        throw new ForbiddenException("無權限操作此文件");
    }

    private void requireShareManagement(Document document, User currentUser) {
        if (isPrivileged(currentUser) || isOwner(currentUser, document)) {
            return;
        }
        throw new ForbiddenException("無權限管理此文件分享");
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == UserRole.ADMIN || user.getRole() == UserRole.MANAGER;
    }

    private boolean isOwner(User user, Document document) {
        return document.getCreatedBy() != null
                && document.getCreatedBy().getId() != null
                && document.getCreatedBy().getId().equals(user.getId());
    }

    /**
     * 將字串狀態轉為文件狀態列舉。
     *
     * @param status 文件狀態字串
     * @return 文件狀態列舉
     * @throws BadRequestException 若狀態值無效
     */
    private DocumentStatus parseStatus(String status) {
        log.debug("Parsing document status: status={}", status);
        try {
            return DocumentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            log.warn("Invalid document status received: {}", status);
            throw new BadRequestException("Invalid document status");
        }
    }

    private DocumentSharePermission parseSharePermission(String permission) {
        try {
            return DocumentSharePermission.valueOf(permission.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new BadRequestException("Invalid share permission");
        }
    }

    private DocumentResponse toSharedDocumentResponse(DocumentShare share) {
        return toAccessibleResponse(
                share.getDocument(),
                share.getSharedWith(),
                AccessContext.shared(share.getPermission(), share.getCreatedBy())
        );
    }

    private DocumentResponse toAccessibleResponse(Document document, User currentUser, AccessContext existingContext) {
        AccessContext accessContext = existingContext != null ? existingContext : requireViewAccess(document, currentUser);
        DocumentResponse response = toResponse(document);
        applyAccessContext(response, accessContext);
        return response;
    }

    private void applyAccessContext(DocumentResponse response, AccessContext accessContext) {
        response.setAccessLevel(accessContext.accessLevel.name());
        response.setSharedBy(accessContext.sharedBy);
    }

    private DocumentShareItemResponse toShareItemResponse(DocumentShare share) {
        return DocumentShareItemResponse.builder()
                .id(share.getId())
                .documentId(share.getDocument().getId())
                .userId(share.getSharedWith().getId())
                .username(share.getSharedWith().getUsername())
                .email(share.getSharedWith().getEmail())
                .permission(share.getPermission().name())
                .sharedBy(share.getCreatedBy() != null ? share.getCreatedBy().getUsername() : null)
                .createdAt(share.getCreatedAt())
                .build();
    }

    /**
     * 將文件實體轉為回應物件。
     *
     * @param document 文件實體
     * @return 文件回應資料
     */
    private DocumentResponse toResponse(Document document) {
        log.trace("Converting document entity to response: documentId={}, title={}", document.getId(), document.getTitle());
        return DocumentResponse.builder()
                .id(document.getId())
                .folderId(document.getFolder() != null ? document.getFolder().getId() : null)
                .title(document.getTitle())
                .description(document.getDescription())
                .fileName(document.getFileName())
                .storedFileName(document.getStoredFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .version(document.getVersion())
                .status(document.getStatus().name())
                .createdBy(document.getCreatedBy() != null ? document.getCreatedBy().getId() : null)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .accessLevel(null)
                .sharedBy(null)
                .build();
    }

    private record AccessContext(DocumentAccessLevel accessLevel, String sharedBy) {
        private static AccessContext owner() {
            return new AccessContext(DocumentAccessLevel.OWNER, null);
        }

        private static AccessContext admin() {
            return new AccessContext(DocumentAccessLevel.ADMIN, null);
        }

        private static AccessContext shared(DocumentSharePermission permission, User sharedByUser) {
            return new AccessContext(
                    permission == DocumentSharePermission.EDIT ? DocumentAccessLevel.EDIT : DocumentAccessLevel.VIEW,
                    sharedByUser != null ? sharedByUser.getUsername() : null
            );
        }
    }
}
