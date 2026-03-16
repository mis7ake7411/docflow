package com.docflow.auth.service;

public interface AuthTokenBlacklistService {

    void blacklist(String token, long ttlSeconds);

    boolean isBlacklisted(String token);
}
