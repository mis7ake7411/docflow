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

/**
 * 提供文件管理與下載 API。
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Document", description = "Document metadata and file storage APIs")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;

    /**
     * 建立文件基本資料。
     *
     * @param request 建立資料
     * @return 建立後的文件資訊
     */
    @Operation(summary = "Create document metadata")
    @PostMapping
    public ApiResponse<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest request) {
        return ApiResponse.success(documentService.create(request), "Document created successfully");
    }

    /**
     * 上傳文件檔案內容。
     *
     * @param id 文件編號
     * @param file 上傳檔案
     * @return 更新後的文件資訊
     */
    @Operation(summary = "Upload document file")
    @PostMapping("/{id}/upload")
    public ApiResponse<DocumentResponse> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(id, file), "Document uploaded successfully");
    }

    /**
     * 取得所有文件列表。
     *
     * @return 文件列表
     */
    @Operation(summary = "List documents")
    @GetMapping
    public ApiResponse<List<DocumentResponse>> getAll() {
        return ApiResponse.success(documentService.getAll());
    }

    /**
     * 取得指定文件明細。
     *
     * @param id 文件編號
     * @return 文件資訊
     */
    @Operation(summary = "Get document detail")
    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(documentService.getById(id));
    }

    /**
     * 更新文件基本資料。
     *
     * @param id 文件編號
     * @param request 更新資料
     * @return 更新後的文件資訊
     */
    @Operation(summary = "Update document metadata")
    @PutMapping("/{id}")
    public ApiResponse<DocumentResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateDocumentRequest request) {
        return ApiResponse.success(documentService.update(id, request), "Document updated successfully");
    }

    /**
     * 刪除指定文件。
     *
     * @param id 文件編號
     * @return 成功回應
     */
    @Operation(summary = "Delete document")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ApiResponse.success(null, "Document deleted successfully");
    }

    /**
     * 下載指定文件檔案。
     *
     * @param id 文件編號
     * @return 檔案下載回應
     */
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
