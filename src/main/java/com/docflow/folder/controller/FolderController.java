package com.docflow.folder.controller;

import com.docflow.common.response.ApiResponse;
import com.docflow.folder.dto.CreateFolderRequest;
import com.docflow.folder.dto.FolderResponse;
import com.docflow.folder.dto.FolderTreeResponse;
import com.docflow.folder.dto.UpdateFolderRequest;
import com.docflow.folder.service.FolderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    @PostMapping
    public ApiResponse<FolderResponse> create(@Valid @RequestBody CreateFolderRequest request) {
        return ApiResponse.success(folderService.create(request), "Folder created successfully");
    }

    @GetMapping("/tree")
    public ApiResponse<List<FolderTreeResponse>> getTree() {
        return ApiResponse.success(folderService.getTree());
    }

    @PutMapping("/{id}")
    public ApiResponse<FolderResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateFolderRequest request) {
        return ApiResponse.success(folderService.update(id, request), "Folder updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        folderService.delete(id);
        return ApiResponse.success(null, "Folder deleted successfully");
    }
}
