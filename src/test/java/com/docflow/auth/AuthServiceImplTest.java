package com.docflow.auth;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.auth.dto.AuthResponse;
import com.docflow.auth.dto.AuthTokenResponse;
import com.docflow.auth.dto.LoginRequest;
import com.docflow.auth.dto.RefreshRequest;
import com.docflow.auth.dto.RegisterRequest;
import com.docflow.auth.entity.RefreshToken;
import com.docflow.auth.repository.RefreshTokenRepository;
import com.docflow.auth.service.AuthServiceImpl;
import com.docflow.auth.service.AuthTokenBlacklistService;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.exception.UnauthorizedException;
import com.docflow.common.security.JwtService;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private AuthTokenBlacklistService authTokenBlacklistService;
    private ActivityLogService activityLogService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        refreshTokenRepository = Mockito.mock(RefreshTokenRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        authenticationManager = Mockito.mock(AuthenticationManager.class);
        jwtService = Mockito.mock(JwtService.class);
        authTokenBlacklistService = Mockito.mock(AuthTokenBlacklistService.class);
        activityLogService = Mockito.mock(ActivityLogService.class);

        authService = new AuthServiceImpl(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                authenticationManager,
                jwtService,
                authTokenBlacklistService,
                activityLogService
        );
    }

    @Test
    void registerShouldSetDefaultRoleAndStatus() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        Mockito.when(userRepository.existsByUsername("alice")).thenReturn(false);
        Mockito.when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        Mockito.when(passwordEncoder.encode("password123")).thenReturn("hash");
        Mockito.when(jwtService.generateAccessToken(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn("access");
        Mockito.when(jwtService.generateRefreshToken(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn("refresh");
        Mockito.when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        Mockito.when(jwtService.getRefreshTokenExpirationSeconds()).thenReturn(3600L);
        Mockito.when(refreshTokenRepository.save(Mockito.any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(userRepository.save(Mockito.any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        AuthResponse response = authService.register(request);

        assertThat(response.getUser().getRole()).isEqualTo("USER");
        assertThat(response.getUser().getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void registerShouldRejectDuplicateUsernameOrEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        Mockito.when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void loginShouldRejectInactiveUser() {
        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("password123");

        User inactive = User.builder()
                .id(1L)
                .username("alice")
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.INACTIVE)
                .mustChangePassword(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Mockito.when(authenticationManager.authenticate(Mockito.any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("alice", "password123"));
        Mockito.when(userRepository.findByUsername("alice")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void refreshShouldRotateRefreshTokenAndInvalidateOldToken() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("old-refresh-token");

        User user = User.builder()
                .id(1L)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .mustChangePassword(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        RefreshToken currentToken = RefreshToken.builder()
                .id(10L)
                .user(user)
                .token("old-refresh-token")
                .expiredAt(LocalDateTime.now().plusMinutes(30))
                .revokedFlag(false)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        Mockito.when(refreshTokenRepository.findByToken("old-refresh-token"))
                .thenReturn(Optional.of(currentToken));
        Mockito.when(jwtService.generateAccessToken(1L, "alice", "USER"))
                .thenReturn("new-access-token");
        Mockito.when(jwtService.generateRefreshToken(1L, "alice", "USER"))
                .thenReturn("new-refresh-token");
        Mockito.when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        Mockito.when(jwtService.getRefreshTokenExpirationSeconds()).thenReturn(7200L);
        Mockito.when(refreshTokenRepository.save(Mockito.any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AuthTokenResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        Mockito.verify(refreshTokenRepository, Mockito.times(1)).save(captor.capture());

        RefreshToken savedToken = captor.getValue();
        assertThat(savedToken.getToken()).isEqualTo("new-refresh-token");
        assertThat(savedToken.isRevokedFlag()).isFalse();
        assertThat(savedToken.getUser()).isEqualTo(user);
        assertThat(savedToken.getId()).isEqualTo(10L);
    }

    @Test
    void refreshShouldRejectInactiveUser() {
        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken("refresh-token");

        User inactiveUser = User.builder()
                .id(2L)
                .username("inactive")
                .email("inactive@example.com")
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.INACTIVE)
                .mustChangePassword(false)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .build();

        RefreshToken currentToken = RefreshToken.builder()
                .id(11L)
                .user(inactiveUser)
                .token("refresh-token")
                .expiredAt(LocalDateTime.now().plusMinutes(30))
                .revokedFlag(false)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        Mockito.when(refreshTokenRepository.findByToken("refresh-token"))
                .thenReturn(Optional.of(currentToken));

        assertThatThrownBy(() -> authService.refresh(request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("User is inactive");

        Mockito.verify(refreshTokenRepository, Mockito.never()).save(Mockito.any(RefreshToken.class));
    }
}
