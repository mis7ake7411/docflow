package com.docflow.document.repository;

import com.docflow.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByDeletedFlagFalseOrderByCreatedAtDesc();

    Optional<Document> findByIdAndDeletedFlagFalse(Long id);
}
