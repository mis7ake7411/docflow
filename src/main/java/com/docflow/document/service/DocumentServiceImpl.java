package com.docflow.document.service;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.security.SecurityUtils;
import com.docflow.document.dto.CreateDocumentRequest;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.UpdateDocumentRequest;
import com.docflow.document.entity.Document;
import com.docflow.document.entity.DocumentStatus;
import com.docflow.document.repository.DocumentRepository;
import com.docflow.document.storage.LocalFileStorageService;
import com.docflow.document.storage.StoredFileResult;
import com.docflow.document.service.DocumentCacheService;
import com.docflow.folder.entity.Folder;
import com.docflow.folder.repository.FolderRepository;
import com.docflow.user.entity.User;
import com.docflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * {@link DocumentService} 的預設實作，負責文件資料、檔案儲存與快取同步。
 */
@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
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
        User currentUser = getCurrentUser();
        Folder folder = resolveFolder(request.getFolderId());

        Document document = Document.builder()
                .folder(folder)
                .title(request.getTitle())
                .description(request.getDescription())
                .version(1)
                .status(parseStatus(request.getStatus()))
                .createdBy(currentUser)
                .deletedFlag(false)
                .build();

        Document saved = documentRepository.save(document);
        DocumentResponse response = toResponse(saved);
        documentCacheService.evictDocumentDetail(response.getId());
        activityLogService.log(currentUser.getId(), "DOCUMENT", saved.getId(), "CREATE", java.util.Map.of(
                "title", saved.getTitle(),
                "status", saved.getStatus().name()
        ));
        return response;
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
        Document document = getActiveDocument(id);
        StoredFileResult storedFile = localFileStorageService.store(file);

        document.setFileName(storedFile.getOriginalFileName());
        document.setStoredFileName(storedFile.getStoredFileName());
        document.setContentType(storedFile.getContentType());
        document.setFileSize(storedFile.getFileSize());
        document.setVersion(document.getVersion() + 1);

        Document saved = documentRepository.save(document);
        DocumentResponse response = toResponse(saved);
        documentCacheService.evictDocumentDetail(id);
        activityLogService.log(getCurrentUser().getId(), "DOCUMENT", saved.getId(), "UPLOAD", java.util.Map.of(
                "fileName", saved.getFileName(),
                "storedFileName", saved.getStoredFileName(),
                "version", saved.getVersion()
        ));
        return response;
    }

    /**
     * 取得所有未刪除文件。
     *
     * @return 文件列表
     */
    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAll() {
        return documentRepository.findAllByDeletedFlagFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
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
        return documentCacheService.getDocumentDetail(id)
                .orElseGet(() -> {
                    DocumentResponse response = toResponse(getActiveDocument(id));
                    documentCacheService.cacheDocumentDetail(id, response);
                    return response;
                });
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
        Document document = getActiveDocument(id);
        Folder folder = resolveFolder(request.getFolderId());

        document.setFolder(folder);
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setStatus(parseStatus(request.getStatus()));
        document.setVersion(document.getVersion() + 1);

        Document saved = documentRepository.save(document);
        DocumentResponse response = toResponse(saved);
        documentCacheService.evictDocumentDetail(id);
        activityLogService.log(getCurrentUser().getId(), "DOCUMENT", saved.getId(), "UPDATE", java.util.Map.of(
                "title", saved.getTitle(),
                "status", saved.getStatus().name(),
                "version", saved.getVersion()
        ));
        return response;
    }

    /**
     * 軟刪除指定文件並清除明細快取。
     *
     * @param id 文件編號
     */
    @Override
    @Transactional
    public void delete(Long id) {
        Document document = getActiveDocument(id);
        document.setDeletedFlag(true);
        documentRepository.save(document);
        documentCacheService.evictDocumentDetail(id);
        activityLogService.log(getCurrentUser().getId(), "DOCUMENT", document.getId(), "DELETE", java.util.Map.of(
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
    @Transactional(readOnly = true)
    public Resource download(Long id) {
        DocumentResponse response = getById(id);
        if (response.getStoredFileName() == null || response.getStoredFileName().isBlank()) {
            throw new BadRequestException("Document file has not been uploaded");
        }
        Long currentUserId = getCurrentUser().getId();
        documentCacheService.recordDocumentView(currentUserId, response);
        activityLogService.log(currentUserId, "DOCUMENT", response.getId(), "DOWNLOAD", java.util.Map.of(
                "fileName", response.getFileName(),
                "version", response.getVersion()
        ));
        return localFileStorageService.loadAsResource(response.getStoredFileName());
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
     * 取得目前登入使用者。
     *
     * @return 目前登入使用者
     */
    private User getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Current user not found"));
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
        return folderRepository.findByIdAndDeletedFlagFalse(folderId)
                .orElseThrow(() -> new BadRequestException("Folder not found"));
    }

    /**
     * 將字串狀態轉為文件狀態列舉。
     *
     * @param status 文件狀態字串
     * @return 文件狀態列舉
     */
    private DocumentStatus parseStatus(String status) {
        try {
            return DocumentStatus.valueOf(status.toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid document status");
        }
    }

    /**
     * 將文件實體轉為回應物件。
     *
     * @param document 文件實體
     * @return 文件回應資料
     */
    private DocumentResponse toResponse(Document document) {
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
                .createdBy(document.getCreatedBy().getId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
