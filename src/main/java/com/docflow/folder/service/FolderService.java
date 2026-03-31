package com.docflow.folder.service;

import com.docflow.folder.dto.CreateFolderRequest;
import com.docflow.folder.dto.FolderResponse;
import com.docflow.folder.dto.FolderTreeResponse;
import com.docflow.folder.dto.ReorderFoldersRequest;
import com.docflow.folder.dto.UpdateFolderRequest;

import java.util.List;

/**
 * 提供資料夾管理相關操作。
 */
public interface FolderService {

    /**
     * 建立資料夾。
     *
     * @param request 建立資料
     * @return 建立後的資料夾資訊
     */
    FolderResponse create(CreateFolderRequest request);

    /**
     * 取得資料夾樹狀結構。
     *
     * @return 根節點起始的資料夾樹
     */
    List<FolderTreeResponse> getTree();

    /**
     * 更新指定資料夾。
     *
     * @param id 資料夾編號
     * @param request 更新資料
     * @return 更新後的資料夾資訊
     */
    FolderResponse update(Long id, UpdateFolderRequest request);

    void reorder(ReorderFoldersRequest request);

    /**
     * 刪除指定資料夾。
     *
     * @param id 資料夾編號
     */
    void delete(Long id);
}
