package com.docflow.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDocumentRequest {

    private Long folderId;

    @NotBlank(message = "Document title is required")
    @Size(max = 200, message = "Document title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Document description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Document status is required")
    private String status;
}
