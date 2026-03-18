package com.docflow.document.service;

import com.docflow.document.dto.CreateDocumentRequest;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.UpdateDocumentRequest;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 提供文件建立、維護與下載操作。
 */
public interface DocumentService {

    /**
     * 建立文件基本資料。
     *
     * @param request 建立資料
     * @return 建立後的文件資訊
     */
    DocumentResponse create(CreateDocumentRequest request);

    /**
     * 上傳文件內容並更新版本。
     *
     * @param id 文件編號
     * @param file 上傳檔案
     * @return 更新後的文件資訊
     */
    DocumentResponse upload(Long id, MultipartFile file);

    /**
     * 取得所有未刪除文件。
     *
     * @return 文件列表
     */
    List<DocumentResponse> getAll();

    /**
     * 取得指定文件明細。
     *
     * @param id 文件編號
     * @return 文件資訊
     */
    DocumentResponse getById(Long id);

    /**
     * 更新文件基本資料。
     *
     * @param id 文件編號
     * @param request 更新資料
     * @return 更新後的文件資訊
     */
    DocumentResponse update(Long id, UpdateDocumentRequest request);

    /**
     * 軟刪除指定文件。
     *
     * @param id 文件編號
     */
    void delete(Long id);

    /**
     * 下載指定文件內容。
     *
     * @param id 文件編號
     * @return 可供下載的檔案資源
     */
    Resource download(Long id);
}
