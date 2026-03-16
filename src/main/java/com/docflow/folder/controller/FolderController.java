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

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
@Tag(name = "Folder", description = "Folder management APIs")
@SecurityRequirement(name = "bearerAuth")
public class FolderController {

    private final FolderService folderService;

    @Operation(summary = "Create folder")
    @PostMapping
    public ApiResponse<FolderResponse> create(@Valid @RequestBody CreateFolderRequest request) {
        return ApiResponse.success(folderService.create(request), "Folder created successfully");
    }

    @Operation(summary = "Get folder tree")
    @GetMapping("/tree")
    public ApiResponse<List<FolderTreeResponse>> getTree() {
        return ApiResponse.success(folderService.getTree());
    }

    @Operation(summary = "Update folder")
    @PutMapping("/{id}")
    public ApiResponse<FolderResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateFolderRequest request) {
        return ApiResponse.success(folderService.update(id, request), "Folder updated successfully");
    }

    @Operation(summary = "Delete folder")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        folderService.delete(id);
        return ApiResponse.success(null, "Folder deleted successfully");
    }
}
