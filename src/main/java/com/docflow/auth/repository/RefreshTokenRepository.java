package com.docflow.auth.repository;

import com.docflow.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 提供 refresh token 資料存取操作。
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 依 token 字串查詢 refresh token。
     *
     * @param token refresh token 字串
     * @return refresh token 資料
     */
    Optional<RefreshToken> findByToken(String token);
}
