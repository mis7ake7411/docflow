package com.docflow.document.service;

import com.docflow.common.exception.BadRequestException;
import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.HotDocumentItem;
import com.docflow.document.dto.RecentViewItem;
import com.docflow.document.entity.Document;
import com.docflow.document.repository.DocumentRepository;
import com.docflow.infra.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * {@link DocumentCacheService} 的 Redis 實作，負責文件快取、統計與分散式鎖。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentCacheServiceImpl implements DocumentCacheService {

    private static final Duration DOCUMENT_DETAIL_TTL = Duration.ofMinutes(10);
    private static final Duration DOCUMENT_UPDATE_LOCK_TTL = Duration.ofSeconds(30);
    private static final int RECENT_VIEW_LIMIT = 20;
    private static final int HOT_DOCUMENT_LIMIT = 10;

    private final RedisTemplate<String, Object> redisTemplate;
    private final DocumentRepository documentRepository;

    /**
     * 取得文件明細快取。
     *
     * @param documentId 文件編號
     * @return 文件明細快取資料
     */
    @Override
    public Optional<DocumentResponse> getDocumentDetail(Long documentId) {
        Object value = redisTemplate.opsForValue().get(RedisKeys.documentDetail(documentId));
        if (value instanceof DocumentResponse response) {
            log.debug("Document cache hit: documentId={}", documentId);
            return Optional.of(response);
        }
        log.debug("Document cache miss: documentId={}", documentId);
        return Optional.empty();
    }

    /**
     * 寫入文件明細快取。
     *
     * @param documentId 文件編號
     * @param response 文件明細資料
     */
    @Override
    public void cacheDocumentDetail(Long documentId, DocumentResponse response) {
        log.debug("Caching document detail: documentId={}, ttlSeconds={}", documentId, DOCUMENT_DETAIL_TTL.getSeconds());
        redisTemplate.opsForValue().set(RedisKeys.documentDetail(documentId), response, DOCUMENT_DETAIL_TTL);
    }

    /**
     * 清除文件明細快取。
     *
     * @param documentId 文件編號
     */
    @Override
    public void evictDocumentDetail(Long documentId) {
        log.debug("Evicting document detail cache: documentId={}", documentId);
        redisTemplate.delete(RedisKeys.documentDetail(documentId));
    }

    /**
     * 記錄使用者最近瀏覽文件，並累計熱門文件分數。
     *
     * @param userId 使用者編號
     * @param response 文件資料
     */
    @Override
    public void recordDocumentView(Long userId, DocumentResponse response) {
        log.debug("Recording document view: userId={}, documentId={}", userId, response.getId());
        long now = Instant.now().toEpochMilli();
        String recentViewsKey = RedisKeys.recentViews(userId);
        String hotDocumentsKey = RedisKeys.hotDocuments();

        redisTemplate.opsForZSet().add(recentViewsKey, String.valueOf(response.getId()), now);
        redisTemplate.opsForZSet().removeRange(recentViewsKey, 0, Math.max(0, sizeOfZSet(recentViewsKey) - RECENT_VIEW_LIMIT - 1));

        redisTemplate.opsForZSet().incrementScore(hotDocumentsKey, String.valueOf(response.getId()), 1.0d);
    }

    /**
     * 取得使用者最近瀏覽的文件列表。
     *
     * @param userId 使用者編號
     * @return 最近瀏覽文件資料
     */
    @Override
    public List<RecentViewItem> getRecentViews(Long userId) {
        log.debug("Loading recent document views: userId={}", userId);
        String key = RedisKeys.recentViews(userId);
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, RECENT_VIEW_LIMIT - 1);

        List<RecentViewItem> results = new ArrayList<>();
        if (tuples == null) {
            return results;
        }

        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long documentId = Long.parseLong(String.valueOf(tuple.getValue()));
            resolveDocument(documentId).ifPresent(document -> results.add(RecentViewItem.builder()
                    .documentId(document.getId())
                    .title(document.getTitle())
                    .status(document.getStatus())
                    .score(tuple.getScore())
                    .build()));
        }

        return results;
    }

    /**
     * 取得熱門文件列表。
     *
     * @return 熱門文件資料
     */
    @Override
    public List<HotDocumentItem> getHotDocuments() {
        log.debug("Loading hot documents from cache");
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(RedisKeys.hotDocuments(), 0, HOT_DOCUMENT_LIMIT - 1);

        List<HotDocumentItem> results = new ArrayList<>();
        if (tuples == null) {
            return results;
        }

        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long documentId = Long.parseLong(String.valueOf(tuple.getValue()));
            resolveDocument(documentId).ifPresent(document -> results.add(HotDocumentItem.builder()
                    .documentId(document.getId())
                    .title(document.getTitle())
                    .status(document.getStatus())
                    .score(tuple.getScore())
                    .build()));
        }

        return results;
    }

    /**
     * 將 access token 加入黑名單。
     *
     * @param token access token
     * @param ttlSeconds 黑名單保留秒數
     */
    @Override
    public void blacklistAccessToken(String token, long ttlSeconds) {
        log.info("Blacklisting access token: ttlSeconds={}", ttlSeconds);
        redisTemplate.opsForValue().set(RedisKeys.authBlacklist(token), Boolean.TRUE, Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 判斷 access token 是否已在黑名單中。
     *
     * @param token access token
     * @return 若已在黑名單中則回傳 {@code true}
     */
    @Override
    public boolean isAccessTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().get(RedisKeys.authBlacklist(token)));
    }

    /**
     * 嘗試取得文件更新鎖。
     *
     * @param documentId 文件編號
     * @return 若成功取得鎖則回傳 {@code true}
     */
    @Override
    public boolean acquireDocumentUpdateLock(Long documentId) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(
                RedisKeys.documentUpdateLock(documentId),
                "locked",
                DOCUMENT_UPDATE_LOCK_TTL
        );
        log.debug("Acquire document update lock: documentId={}, acquired={}", documentId, Boolean.TRUE.equals(result));
        return Boolean.TRUE.equals(result);
    }

    /**
     * 釋放文件更新鎖。
     *
     * @param documentId 文件編號
     */
    @Override
    public void releaseDocumentUpdateLock(Long documentId) {
        log.debug("Release document update lock: documentId={}", documentId);
        redisTemplate.delete(RedisKeys.documentUpdateLock(documentId));
    }

    /**
     * 取得 sorted set 目前元素數量；若 key 不存在則視為 0。
     *
     * @param key Redis key
     * @return 元素數量
     */
    private long sizeOfZSet(String key) {
        log.trace("Getting zset size: key={}", key);
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size == null ? 0 : size;
    }

    /**
     * 嘗試從快取或資料庫解析文件，並在快取未命中時回寫快取。
     *
     * @param documentId 文件編號
     * @return 文件回應資料
     */
    private Optional<DocumentResponse> resolveDocument(Long documentId) {
        log.trace("Resolving document: documentId={}", documentId);
        return getDocumentDetail(documentId)
                .or(() -> {
                    log.trace("Document not in cache, querying database: documentId={}", documentId);
                    return documentRepository.findByIdAndDeletedFlagFalse(documentId).map(this::toResponse);
                })
                .map(response -> {
                    cacheDocumentDetail(documentId, response);
                    return response;
                });
    }

    /**
     * 將文件實體轉為回應物件。
     *
     * @param document 文件實體
     * @return 文件回應資料
     * @throws BadRequestException 若文件建立者不存在
     */
    private DocumentResponse toResponse(Document document) {
        log.trace("Converting document entity to response: documentId={}, title={}", document.getId(), document.getTitle());
        if (document.getCreatedBy() == null) {
            log.error("Document creator is null: documentId={}", document.getId());
            throw new BadRequestException("Document creator not found");
        }

        return DocumentResponse.builder()
                .id(document.getId())
                .folderId(document.getFolder() != null ? document.getFolder().getId() : null)
                .title(document.getTitle())
                .description(document.getDescription())
                .fileName(document.getFileName())
                .storedFileName(document.getStoredFileName())
                .contentType(document.getContentType())
                .fileSize(document.getFileSize())
                .version(document.getVersion())
                .status(document.getStatus().name())
                .createdBy(document.getCreatedBy().getId())
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
