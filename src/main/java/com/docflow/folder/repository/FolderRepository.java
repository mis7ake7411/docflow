package com.docflow.folder.repository;

import com.docflow.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 提供資料夾資料存取操作。
 */
public interface FolderRepository extends JpaRepository<Folder, Long> {

    /**
     * 取得所有未刪除資料夾，依排序欄位與編號排序。
     *
     * @return 資料夾列表
     */
    List<Folder> findAllByDeletedFlagFalseOrderBySortOrderAscIdAsc();

    /**
     * 依編號查詢未刪除資料夾。
     *
     * @param id 資料夾編號
     * @return 資料夾資料
     */
    Optional<Folder> findByIdAndDeletedFlagFalse(Long id);

    /**
     * 判斷指定父資料夾是否仍有未刪除子資料夾。
     *
     * @param parentId 父資料夾編號
     * @return 若存在未刪除子資料夾則回傳 {@code true}
     */
    boolean existsByParentIdAndDeletedFlagFalse(Long parentId);
}
