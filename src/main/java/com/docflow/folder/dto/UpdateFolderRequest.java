package com.docflow.folder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateFolderRequest {

    @NotBlank(message = "Folder name is required")
    @Size(max = 100, message = "Folder name must not exceed 100 characters")
    private String name;

    private Long parentId;

    @NotNull(message = "Sort order is required")
    private Integer sortOrder;
}
