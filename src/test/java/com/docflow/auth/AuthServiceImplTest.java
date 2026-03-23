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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthTokenBlacklistService authTokenBlacklistService;

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerShouldSetDefaultRoleAndStatus() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(userRepository.save(Mockito.any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubAuthTokens();
        doNothing().when(activityLogService).log(Mockito.<Long>any(), anyString(), Mockito.<Long>any(), anyString(), any());

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

        when(userRepository.existsByUsername("alice")).thenReturn(true);

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
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("alice", "password123"));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(UnauthorizedException.class);
    }

    private void stubAuthTokens() {
        when(jwtService.generateAccessToken(Mockito.<Long>any(), anyString(), anyString())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(Mockito.<Long>any(), anyString(), anyString())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpirationSeconds()).thenReturn(3600L);
        when(jwtService.getRefreshTokenExpirationSeconds()).thenReturn(7200L);
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
