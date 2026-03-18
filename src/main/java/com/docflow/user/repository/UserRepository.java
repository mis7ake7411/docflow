package com.docflow.user.repository;

import com.docflow.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 提供使用者資料存取操作。
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 檢查使用者名稱是否已存在。
     *
     * @param username 使用者名稱
     * @return 若已存在則回傳 {@code true}
     */
    boolean existsByUsername(String username);

    /**
     * 檢查電子郵件是否已存在。
     *
     * @param email 電子郵件
     * @return 若已存在則回傳 {@code true}
     */
    boolean existsByEmail(String email);

    /**
     * 依使用者名稱查詢使用者。
     *
     * @param username 使用者名稱
     * @return 使用者資料
     */
    Optional<User> findByUsername(String username);

    /**
     * 依電子郵件查詢使用者。
     *
     * @param email 電子郵件
     * @return 使用者資料
     */
    Optional<User> findByEmail(String email);
}
