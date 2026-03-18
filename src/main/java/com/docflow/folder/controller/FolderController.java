package com.docflow.folder.controller;

import com.docflow.common.response.ApiResponse;
import com.docflow.folder.dto.CreateFolderRequest;
import com.docflow.folder.dto.FolderResponse;
import com.docflow.folder.dto.FolderTreeResponse;
import com.docflow.folder.dto.UpdateFolderRequest;
import com.docflow.folder.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提供資料夾管理 API。
 */
@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Tag(name = "Folder", description = "Folder management APIs")
@SecurityRequirement(name = "bearerAuth")
public class FolderController {

    private final FolderService folderService;

    /**
     * 建立資料夾。
     *
     * @param request 建立資料
     * @return 建立後的資料夾資訊
     */
    @Operation(summary = "Create folder")
    @PostMapping
    public ApiResponse<FolderResponse> create(@Valid @RequestBody CreateFolderRequest request) {
        return ApiResponse.success(folderService.create(request), "Folder created successfully");
    }

    /**
     * 取得資料夾樹狀結構。
     *
     * @return 資料夾樹
     */
    @Operation(summary = "Get folder tree")
    @GetMapping("/tree")
    public ApiResponse<List<FolderTreeResponse>> getTree() {
        return ApiResponse.success(folderService.getTree());
    }

    /**
     * 更新指定資料夾。
     *
     * @param id 資料夾編號
     * @param request 更新資料
     * @return 更新後的資料夾資訊
     */
    @Operation(summary = "Update folder")
    @PutMapping("/{id}")
    public ApiResponse<FolderResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateFolderRequest request) {
        return ApiResponse.success(folderService.update(id, request), "Folder updated successfully");
    }

    /**
     * 刪除指定資料夾。
     *
     * @param id 資料夾編號
     * @return 成功回應
     */
    @Operation(summary = "Delete folder")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        folderService.delete(id);
        return ApiResponse.success(null, "Folder deleted successfully");
    }
}
