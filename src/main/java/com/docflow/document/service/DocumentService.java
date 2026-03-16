package com.docflow.document.service;

import com.docflow.document.dto.CreateDocumentRequest;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.UpdateDocumentRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {

    DocumentResponse create(CreateDocumentRequest request);

    DocumentResponse upload(Long id, MultipartFile file);

    List<DocumentResponse> getAll();

    DocumentResponse getById(Long id);

    DocumentResponse update(Long id, UpdateDocumentRequest request);

    void delete(Long id);

    Resource download(Long id);
}
