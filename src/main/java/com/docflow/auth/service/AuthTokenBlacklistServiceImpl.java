package com.docflow.auth.service;

import com.docflow.document.service.DocumentCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthTokenBlacklistServiceImpl implements AuthTokenBlacklistService {

    private final DocumentCacheService documentCacheService;

    @Override
    public void blacklist(String token, long ttlSeconds) {
        documentCacheService.blacklistAccessToken(token, ttlSeconds);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return documentCacheService.isAccessTokenBlacklisted(token);
    }
}
