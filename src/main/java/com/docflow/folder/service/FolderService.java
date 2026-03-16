package com.docflow.folder.service;

import com.docflow.folder.dto.CreateFolderRequest;
import com.docflow.folder.dto.FolderResponse;
import com.docflow.folder.dto.FolderTreeResponse;
import com.docflow.folder.dto.UpdateFolderRequest;

import java.util.List;

public interface FolderService {

    FolderResponse create(CreateFolderRequest request);

    List<FolderTreeResponse> getTree();

    FolderResponse update(Long id, UpdateFolderRequest request);

    void delete(Long id);
}
