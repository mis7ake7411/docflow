package com.docflow.document.service;

import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.HotDocumentItem;
import com.docflow.document.dto.RecentViewItem;
import com.docflow.infra.redis.RedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentCacheServiceImpl implements DocumentCacheService {

    private static final Duration DOCUMENT_DETAIL_TTL = Duration.ofMinutes(10);
    private static final Duration DOCUMENT_UPDATE_LOCK_TTL = Duration.ofSeconds(30);
    private static final int RECENT_VIEW_LIMIT = 20;
    private static final int HOT_DOCUMENT_LIMIT = 10;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Optional<DocumentResponse> getDocumentDetail(Long documentId) {
        Object value = redisTemplate.opsForValue().get(RedisKeys.documentDetail(documentId));
        if (value instanceof DocumentResponse response) {
            return Optional.of(response);
        }
        return Optional.empty();
    }

    @Override
    public void cacheDocumentDetail(Long documentId, DocumentResponse response) {
        redisTemplate.opsForValue().set(RedisKeys.documentDetail(documentId), response, DOCUMENT_DETAIL_TTL);
    }

    @Override
    public void evictDocumentDetail(Long documentId) {
        redisTemplate.delete(RedisKeys.documentDetail(documentId));
    }

    @Override
    public void recordDocumentView(Long userId, DocumentResponse response) {
        long now = Instant.now().toEpochMilli();
        String recentViewsKey = RedisKeys.recentViews(userId);
        String hotDocumentsKey = RedisKeys.hotDocuments();

        redisTemplate.opsForZSet().add(recentViewsKey, String.valueOf(response.getId()), now);
        redisTemplate.opsForZSet().removeRange(recentViewsKey, 0, Math.max(0, sizeOfZSet(recentViewsKey) - RECENT_VIEW_LIMIT - 1));

        redisTemplate.opsForZSet().incrementScore(hotDocumentsKey, String.valueOf(response.getId()), 1.0d);
    }

    @Override
    public List<RecentViewItem> getRecentViews(Long userId) {
        String key = RedisKeys.recentViews(userId);
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, RECENT_VIEW_LIMIT - 1);

        List<RecentViewItem> results = new ArrayList<>();
        if (tuples == null) {
            return results;
        }

        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long documentId = Long.parseLong(String.valueOf(tuple.getValue()));
            getDocumentDetail(documentId).ifPresent(document -> results.add(RecentViewItem.builder()
                    .documentId(document.getId())
                    .title(document.getTitle())
                    .status(document.getStatus())
                    .score(tuple.getScore())
                    .build()));
        }

        return results;
    }

    @Override
    public List<HotDocumentItem> getHotDocuments() {
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(RedisKeys.hotDocuments(), 0, HOT_DOCUMENT_LIMIT - 1);

        List<HotDocumentItem> results = new ArrayList<>();
        if (tuples == null) {
            return results;
        }

        for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Long documentId = Long.parseLong(String.valueOf(tuple.getValue()));
            getDocumentDetail(documentId).ifPresent(document -> results.add(HotDocumentItem.builder()
                    .documentId(document.getId())
                    .title(document.getTitle())
                    .status(document.getStatus())
                    .score(tuple.getScore())
                    .build()));
        }

        return results;
    }

    @Override
    public void blacklistAccessToken(String token, long ttlSeconds) {
        redisTemplate.opsForValue().set(RedisKeys.authBlacklist(token), Boolean.TRUE, Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public boolean isAccessTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().get(RedisKeys.authBlacklist(token)));
    }

    @Override
    public boolean acquireDocumentUpdateLock(Long documentId) {
        Boolean result = redisTemplate.opsForValue().setIfAbsent(
                RedisKeys.documentUpdateLock(documentId),
                "locked",
                DOCUMENT_UPDATE_LOCK_TTL
        );
        return Boolean.TRUE.equals(result);
    }

    @Override
    public void releaseDocumentUpdateLock(Long documentId) {
        redisTemplate.delete(RedisKeys.documentUpdateLock(documentId));
    }

    private long sizeOfZSet(String key) {
        Long size = redisTemplate.opsForZSet().zCard(key);
        return size == null ? 0 : size;
    }
}
