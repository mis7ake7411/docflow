package com.docflow.document.service;

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

@Service
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final LocalFileStorageService localFileStorageService;
    private final DocumentCacheService documentCacheService;

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

        DocumentResponse response = toResponse(documentRepository.save(document));
        documentCacheService.evictDocumentDetail(response.getId());
        return response;
    }

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

        DocumentResponse response = toResponse(documentRepository.save(document));
        documentCacheService.evictDocumentDetail(id);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentResponse> getAll() {
        return documentRepository.findAllByDeletedFlagFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

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

        DocumentResponse response = toResponse(documentRepository.save(document));
        documentCacheService.evictDocumentDetail(id);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Document document = getActiveDocument(id);
        document.setDeletedFlag(true);
        documentRepository.save(document);
        documentCacheService.evictDocumentDetail(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(Long id) {
        DocumentResponse response = getById(id);
        if (response.getStoredFileName() == null || response.getStoredFileName().isBlank()) {
            throw new BadRequestException("Document file has not been uploaded");
        }
        documentCacheService.recordDocumentView(getCurrentUser().getId(), response);
        return localFileStorageService.loadAsResource(response.getStoredFileName());
    }

    private Document getActiveDocument(Long id) {
        return documentRepository.findByIdAndDeletedFlagFalse(id)
                .orElseThrow(() -> new BadRequestException("Document not found"));
    }

    private User getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Current user not found"));
    }

    private Folder resolveFolder(Long folderId) {
        if (folderId == null) {
            return null;
        }
        return folderRepository.findByIdAndDeletedFlagFalse(folderId)
                .orElseThrow(() -> new BadRequestException("Folder not found"));
    }

    private DocumentStatus parseStatus(String status) {
        try {
            return DocumentStatus.valueOf(status.toUpperCase());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid document status");
        }
    }

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
