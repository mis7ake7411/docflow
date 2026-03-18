package com.docflow.auth.service;

import com.docflow.document.service.DocumentCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * {@link AuthTokenBlacklistService} 的預設實作，使用快取儲存黑名單 token。
 */
@Service
@RequiredArgsConstructor
public class AuthTokenBlacklistServiceImpl implements AuthTokenBlacklistService {

    private final DocumentCacheService documentCacheService;

    /**
     * 將 token 加入黑名單直到指定存活時間結束。
     *
     * @param token access token
     * @param ttlSeconds 黑名單保留秒數
     */
    @Override
    public void blacklist(String token, long ttlSeconds) {
        documentCacheService.blacklistAccessToken(token, ttlSeconds);
    }

    /**
     * 判斷 token 是否已在黑名單中。
     *
     * @param token access token
     * @return 若已在黑名單中則回傳 {@code true}
     */
    @Override
    public boolean isBlacklisted(String token) {
        return documentCacheService.isAccessTokenBlacklisted(token);
    }
}
