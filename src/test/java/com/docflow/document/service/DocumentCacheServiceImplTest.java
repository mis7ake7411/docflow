package com.docflow.document.service;

import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentCacheServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ZSetOperations<String, Object> zSetOperations;

    private DocumentCacheServiceImpl documentCacheService;

    @BeforeEach
    void setUp() {
        documentCacheService = new DocumentCacheServiceImpl(redisTemplate, documentRepository);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void recordDocumentViewShouldNotTrimWhenRecentViewsWithinLimit() {
        Long userId = 1L;
        DocumentResponse response = DocumentResponse.builder().id(101L).build();

        when(zSetOperations.add(eq("user:recent:view:1"), eq("101"), anyDouble())).thenReturn(true);
        when(zSetOperations.zCard("user:recent:view:1")).thenReturn(1L);

        documentCacheService.recordDocumentView(userId, response);

        verify(zSetOperations, never()).removeRange(eq("user:recent:view:1"), eq(0L), eq(0L));
        verify(zSetOperations).incrementScore("doc:hot", "101", 1.0d);
    }

    @Test
    void recordDocumentViewShouldTrimOnlyOverflowItems() {
        Long userId = 1L;
        DocumentResponse response = DocumentResponse.builder().id(101L).build();

        when(zSetOperations.add(eq("user:recent:view:1"), eq("101"), anyDouble())).thenReturn(true);
        when(zSetOperations.zCard("user:recent:view:1")).thenReturn(21L);

        documentCacheService.recordDocumentView(userId, response);

        verify(zSetOperations).removeRange("user:recent:view:1", 0, 0);
        verify(zSetOperations).incrementScore("doc:hot", "101", 1.0d);
    }
}

