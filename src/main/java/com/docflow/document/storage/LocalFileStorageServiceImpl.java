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

/**
 * {@link LocalFileStorageService} 的本地檔案系統實作，負責文件上傳與下載。
 */
@Service
@Slf4j
public class LocalFileStorageServiceImpl implements LocalFileStorageService {

    private final Path uploadRoot;

    /**
     * 初始化本地檔案儲存服務。
     *
     * @param uploadDir 上傳檔案根目錄路徑（來自組態）
     * @throws IllegalStateException 若目錄建立失敗
     */
    public LocalFileStorageServiceImpl(@Value("${docflow.storage.upload-dir}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
            log.info("File storage initialized at path={}", this.uploadRoot);
        } catch (IOException ex) {
            log.error("Failed to initialize upload directory: path={}", this.uploadRoot, ex);
            throw new IllegalStateException("Could not initialize upload directory", ex);
        }
    }

    /**
     * 儲存上傳的檔案並傳回儲存結果。
     *
     * @param multipartFile 上傳的檔案
     * @return 包含原始檔名、儲存檔名與檔案資訊的結果物件
     * @throws BadRequestException 若上傳檔案為空
     * @throws IllegalStateException 若檔案儲存失敗
     */
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
            log.info("Storing file: originalFilename={}, storedFileName={}, size={}", 
                    originalFilename, storedFileName, multipartFile.getSize());
            Files.copy(multipartFile.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored successfully: storedFileName={}", storedFileName);
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

    /**
     * 以 Spring Resource 形式載入已儲存的檔案。
     *
     * @param storedFileName 儲存檔名
     * @return 可供下載或存取的檔案資源
     * @throws BadRequestException 若儲存檔案不存在或無法讀取
     * @throws IllegalStateException 若資源路徑轉換失敗
     */
    @Override
    public Resource loadAsResource(String storedFileName) {
        try {
            Path filePath = uploadRoot.resolve(storedFileName).normalize();
            log.debug("Loading stored file as resource: storedFileName={}", storedFileName);
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                log.warn("Stored file not found or unreadable: storedFileName={}", storedFileName);
                throw new BadRequestException("Stored file not found");
            }
            log.debug("File resource loaded successfully: storedFileName={}", storedFileName);
            return resource;
        } catch (MalformedURLException ex) {
            log.error("Failed to load file resource: storedFileName={}", storedFileName, ex);
            throw new IllegalStateException("Failed to load file resource", ex);
        }
    }
}
