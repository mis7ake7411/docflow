package com.docflow.document.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface LocalFileStorageService {

    StoredFileResult store(MultipartFile multipartFile);

    Resource loadAsResource(String storedFileName);
}
