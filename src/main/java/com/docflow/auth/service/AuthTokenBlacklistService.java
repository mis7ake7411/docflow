package com.docflow.auth.service;

/**
 * 提供 access token 黑名單管理能力。
 */
public interface AuthTokenBlacklistService {

    /**
     * 將 token 加入黑名單直到指定存活時間結束。
     *
     * @param token access token
     * @param ttlSeconds 黑名單保留秒數
     */
    void blacklist(String token, long ttlSeconds);

    /**
     * 判斷 token 是否已在黑名單中。
     *
     * @param token access token
     * @return 若已在黑名單中則回傳 {@code true}
     */
    boolean isBlacklisted(String token);
}
