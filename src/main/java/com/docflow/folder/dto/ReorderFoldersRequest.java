package com.docflow.folder.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderFoldersRequest {

    private Long parentId;

    @NotEmpty(message = "Ordered folder ids are required")
    private List<Long> orderedFolderIds;
}
