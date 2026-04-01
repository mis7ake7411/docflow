package com.docflow.user.repository;

import com.docflow.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

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

    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email, Pageable pageable);

    @Query("""
            select u
            from User u
            where u.status = :status
              and u.id <> :id
              and (
                :keyword is null
                or trim(:keyword) = ''
                or lower(u.username) like lower(concat('%', :keyword, '%'))
                or lower(u.email) like lower(concat('%', :keyword, '%'))
              )
            order by u.username asc
            """)
    List<User> findShareCandidates(@Param("status") com.docflow.user.entity.UserStatus status,
                                   @Param("id") Long id,
                                   @Param("keyword") String keyword);
}
