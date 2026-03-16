package com.docflow.document.controller;

import com.docflow.common.response.ApiResponse;
import com.docflow.document.dto.CreateDocumentRequest;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.UpdateDocumentRequest;
import com.docflow.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document", description = "Document metadata and file storage APIs")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    @Operation(summary = "Create document metadata")
    @PostMapping
    public ApiResponse<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest request) {
        return ApiResponse.success(documentService.create(request), "Document created successfully");
    }

    @Operation(summary = "Upload document file")
    @PostMapping("/{id}/upload")
    public ApiResponse<DocumentResponse> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(id, file), "Document uploaded successfully");
    }

    @Operation(summary = "List documents")
    @GetMapping
    public ApiResponse<List<DocumentResponse>> getAll() {
        return ApiResponse.success(documentService.getAll());
    }

    @Operation(summary = "Get document detail")
    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(documentService.getById(id));
    }

    @Operation(summary = "Update document metadata")
    @PutMapping("/{id}")
    public ApiResponse<DocumentResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateDocumentRequest request) {
        return ApiResponse.success(documentService.update(id, request), "Document updated successfully");
    }

    @Operation(summary = "Delete document")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.success(null, "Document deleted successfully");
    }

    @Operation(summary = "Download document file")
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = documentService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
