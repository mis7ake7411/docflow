package com.docflow.document.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class StoredFileResult {

    private String originalFileName;
    private String storedFileName;
    private String contentType;
    private long fileSize;
}
