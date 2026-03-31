package com.docflow.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class DocumentShareItemResponse {

    private Long id;
    private Long documentId;
    private Long userId;
    private String username;
    private String email;
    private String permission;
    private String sharedBy;
    private LocalDateTime createdAt;
}
