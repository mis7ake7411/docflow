package com.docflow.document.repository;

import com.docflow.document.entity.DocumentShare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentShareRepository extends JpaRepository<DocumentShare, Long> {

    @Query("""
            select s
            from DocumentShare s
            where s.document.id = :documentId
              and s.sharedWith.id = :userId
            """)
    Optional<DocumentShare> findByDocumentIdAndSharedWithUserId(@Param("documentId") Long documentId,
                                                                @Param("userId") Long userId);

    @Query("""
            select s
            from DocumentShare s
            where s.document.id = :documentId
            order by s.createdAt asc
            """)
    List<DocumentShare> findAllByDocumentIdOrderByCreatedAtAsc(@Param("documentId") Long documentId);

    @Query("""
            select s
            from DocumentShare s
            where s.sharedWith.id = :userId
              and s.document.deletedFlag = false
            order by s.createdAt desc
            """)
    Page<DocumentShare> findAllBySharedWithUserIdAndDocumentDeletedFlagFalseOrderByCreatedAtDesc(@Param("userId") Long userId,
                                                                                                  Pageable pageable);

    Optional<DocumentShare> findByIdAndDocumentId(Long id, Long documentId);
}
