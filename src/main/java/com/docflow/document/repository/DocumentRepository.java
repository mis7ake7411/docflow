package com.docflow.document.repository;

import com.docflow.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 提供文件資料存取操作。
 */
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 取得所有未刪除文件，依建立時間由新到舊排序。
     *
     * @return 文件列表
     */
    List<Document> findAllByDeletedFlagFalseOrderByCreatedAtDesc();

    /**
     * 取得分頁的未刪除文件。
     */
    Page<Document> findAllByDeletedFlagFalse(Pageable pageable);

    /**
     * 依編號查詢未刪除文件。
     *
     * @param id 文件編號
     * @return 文件資料
     */
    Optional<Document> findByIdAndDeletedFlagFalse(Long id);
}
