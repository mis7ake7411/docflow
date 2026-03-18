package com.docflow.document.service;

import com.docflow.document.dto.DocumentResponse;
import com.docflow.document.dto.HotDocumentItem;
import com.docflow.document.dto.RecentViewItem;

import java.util.List;
import java.util.Optional;

/**
 * 提供文件快取、瀏覽統計與分散式鎖相關操作。
 */
public interface DocumentCacheService {

    /**
     * 取得文件明細快取。
     *
     * @param documentId 文件編號
     * @return 文件明細快取資料
     */
    Optional<DocumentResponse> getDocumentDetail(Long documentId);

    /**
     * 寫入文件明細快取。
     *
     * @param documentId 文件編號
     * @param response 文件明細資料
     */
    void cacheDocumentDetail(Long documentId, DocumentResponse response);

    /**
     * 清除文件明細快取。
     *
     * @param documentId 文件編號
     */
    void evictDocumentDetail(Long documentId);

    /**
     * 記錄使用者瀏覽文件，並更新熱門文件統計。
     *
     * @param userId 使用者編號
     * @param response 文件資料
     */
    void recordDocumentView(Long userId, DocumentResponse response);

    /**
     * 取得使用者最近瀏覽文件列表。
     *
     * @param userId 使用者編號
     * @return 最近瀏覽文件資料
     */
    List<RecentViewItem> getRecentViews(Long userId);

    /**
     * 取得熱門文件列表。
     *
     * @return 熱門文件資料
     */
    List<HotDocumentItem> getHotDocuments();

    /**
     * 將 access token 加入黑名單。
     *
     * @param token access token
     * @param ttlSeconds 黑名單保留秒數
     */
    void blacklistAccessToken(String token, long ttlSeconds);

    /**
     * 判斷 access token 是否已在黑名單中。
     *
     * @param token access token
     * @return 若已在黑名單中則回傳 {@code true}
     */
    boolean isAccessTokenBlacklisted(String token);

    /**
     * 嘗試取得文件更新鎖。
     *
     * @param documentId 文件編號
     * @return 若成功取得鎖則回傳 {@code true}
     */
    boolean acquireDocumentUpdateLock(Long documentId);

    /**
     * 釋放文件更新鎖。
     *
     * @param documentId 文件編號
     */
    void releaseDocumentUpdateLock(Long documentId);
}
