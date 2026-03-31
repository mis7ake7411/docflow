package com.docflow.document.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ShareDocumentRequest {

    private Long sharedWithUserId;

    @NotBlank(message = "permission is required")
    private String permission;
}
