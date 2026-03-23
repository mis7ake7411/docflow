package com.docflow.auth;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.auth.dto.AuthResponse;
import com.docflow.auth.dto.LoginRequest;
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
}
