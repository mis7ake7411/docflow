package com.docflow.document.service;

import com.docflow.document.dto.CreateDocumentRequest;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.DocumentShareItemResponse;
import com.docflow.document.dto.ShareDocumentRequest;
import com.docflow.document.dto.UpdateDocumentRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentResponse create(CreateDocumentRequest request);

    DocumentResponse upload(Long id, MultipartFile file);

    List<DocumentResponse> getAll();

    com.docflow.common.response.PagedResponse<DocumentResponse> getPaged(int page, int size, Long folderId);

    com.docflow.common.response.PagedResponse<DocumentResponse> getSharedWithMe(int page, int size);

    DocumentResponse getById(Long id);

    DocumentResponse update(Long id, UpdateDocumentRequest request);

    DocumentShareItemResponse createShare(Long documentId, ShareDocumentRequest request);

    List<DocumentShareItemResponse> getShares(Long documentId);

    DocumentShareItemResponse updateShare(Long documentId, Long shareId, ShareDocumentRequest request);

    void deleteShare(Long documentId, Long shareId);

    void delete(Long id);

    Resource download(Long id);
}
