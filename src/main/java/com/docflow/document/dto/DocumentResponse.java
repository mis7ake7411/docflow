package com.docflow.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private Long folderId;
    private String title;
    private String description;
    private String fileName;
    private String storedFileName;
    private String contentType;
    private Long fileSize;
    private Integer version;
    private String status;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String accessLevel;
    private String sharedBy;
}
