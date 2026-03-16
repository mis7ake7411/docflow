package com.docflow.folder.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class FolderTreeResponse {

    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<FolderTreeResponse> children;
}
