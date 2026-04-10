package com.docflow.auth;

import com.docflow.auth.entity.RefreshToken;
import com.docflow.auth.repository.RefreshTokenRepository;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RefreshTokenRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void findByTokenShouldReturnPersistedRefreshToken() {
        User user = entityManager.persistAndFlush(User.builder()
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(false)
                .build());

        RefreshToken saved = refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token("refresh-token")
                .expiredAt(LocalDateTime.now().plusHours(1))
                .revokedFlag(false)
                .build());

        assertThat(refreshTokenRepository.findByToken("refresh-token"))
                .isPresent()
                .get()
                .extracting(RefreshToken::getId, RefreshToken::getToken, RefreshToken::isRevokedFlag)
                .contains(saved.getId(), "refresh-token", false);
    }

    @Test
    void updatingTokenShouldMakeOldValueUnavailable() {
        User user = entityManager.persistAndFlush(User.builder()
                .username("bob")
                .email("bob@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(false)
                .build());

        RefreshToken token = refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token("old-refresh-token")
                .expiredAt(LocalDateTime.now().plusHours(1))
                .revokedFlag(false)
                .build());

        token.setToken("new-refresh-token");
        refreshTokenRepository.save(token);

        assertThat(refreshTokenRepository.findByToken("old-refresh-token")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("new-refresh-token"))
                .isPresent()
                .get()
                .extracting(RefreshToken::getId, RefreshToken::getToken)
                .contains(token.getId(), "new-refresh-token");
    }
}
