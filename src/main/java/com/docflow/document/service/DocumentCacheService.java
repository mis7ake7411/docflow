package com.docflow.document.service;

import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.HotDocumentItem;
import com.docflow.document.dto.RecentViewItem;

import java.util.List;
import java.util.Optional;

public interface DocumentCacheService {

    Optional<DocumentResponse> getDocumentDetail(Long documentId);

    void cacheDocumentDetail(Long documentId, DocumentResponse response);

    void evictDocumentDetail(Long documentId);

    void recordDocumentView(Long userId, DocumentResponse response);

    List<RecentViewItem> getRecentViews(Long userId);

    List<HotDocumentItem> getHotDocuments();

    void blacklistAccessToken(String token, long ttlSeconds);

    boolean isAccessTokenBlacklisted(String token);

    boolean acquireDocumentUpdateLock(Long documentId);

    void releaseDocumentUpdateLock(Long documentId);
}
