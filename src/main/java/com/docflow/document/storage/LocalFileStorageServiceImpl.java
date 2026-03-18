package com.docflow.document.storage;

import com.docflow.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageServiceImpl implements LocalFileStorageService {

    private final Path uploadRoot;

    public LocalFileStorageServiceImpl(@Value("${docflow.storage.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
            log.info("File storage initialized at path={}", this.uploadRoot);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize upload directory", ex);
        }
    }

    @Override
    public StoredFileResult store(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            log.warn("File store rejected because upload is empty");
            throw new BadRequestException("Upload file is required");
        }

        String originalFilename = Objects.requireNonNullElse(multipartFile.getOriginalFilename(), "file");
        String storedFileName = UUID.randomUUID() + "_" + originalFilename.replaceAll("\\s+", "_");
        Path target = uploadRoot.resolve(storedFileName).normalize();

        try {
            log.info("Storing file: originalFilename={}, targetPath={}", originalFilename, target);
            Files.copy(multipartFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            log.error("Failed to store file: originalFilename={}, targetPath={}", originalFilename, target, ex);
            throw new IllegalStateException("Failed to store file", ex);
        }

        return StoredFileResult.builder()
                .originalFileName(originalFilename)
                .storedFileName(storedFileName)
                .contentType(multipartFile.getContentType())
                .fileSize(multipartFile.getSize())
                .build();
    }

    @Override
    public Resource loadAsResource(String storedFileName) {
        try {
            Path filePath = uploadRoot.resolve(storedFileName).normalize();
            log.debug("Loading stored file as resource: storedFileName={}, path={}", storedFileName, filePath);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Stored file not found or unreadable: storedFileName={}, path={}", storedFileName, filePath);
                throw new BadRequestException("Stored file not found");
            }
            return resource;
        } catch (MalformedURLException ex) {
            log.error("Failed to load file resource: storedFileName={}", storedFileName, ex);
            throw new IllegalStateException("Failed to load file resource", ex);
        }
    }
}
