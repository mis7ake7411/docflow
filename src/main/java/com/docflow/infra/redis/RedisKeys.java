package com.docflow.infra.redis;

public final class RedisKeys {

    private RedisKeys() {
    }

    public static String documentDetail(Long documentId) {
        return "doc:detail:" + documentId;
    }

    public static String hotDocuments() {
        return "doc:hot";
    }

    public static String recentViews(Long userId) {
        return "user:recent:view:" + userId;
    }

    public static String authBlacklist(String token) {
        return "auth:blacklist:" + token;
    }

    public static String documentUpdateLock(Long documentId) {
        return "lock:doc:update:" + documentId;
    }
}
