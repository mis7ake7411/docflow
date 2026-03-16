package com.docflow.folder.repository;

import com.docflow.folder.entity.Folder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findAllByDeletedFlagFalseOrderBySortOrderAscIdAsc();

    Optional<Folder> findByIdAndDeletedFlagFalse(Long id);

    boolean existsByParentIdAndDeletedFlagFalse(Long parentId);
}
