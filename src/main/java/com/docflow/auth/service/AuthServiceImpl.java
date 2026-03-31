package com.docflow.auth.service;

import com.docflow.activity.service.ActivityLogService;
import com.docflow.auth.dto.*;
import com.docflow.auth.entity.RefreshToken;
import com.docflow.auth.repository.RefreshTokenRepository;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.exception.UnauthorizedException;
import com.docflow.common.security.JwtService;
import com.docflow.common.security.SecurityUtils;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * {@link AuthService} 的預設實作，負責使用者註冊、登入與權杖生命週期管理。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AuthTokenBlacklistService authTokenBlacklistService;
    private final ActivityLogService activityLogService;

    /**
     * 註冊新使用者並建立初始登入權杖。
     *
     * @param request 註冊資料
     * @return 使用者資訊與權杖
     */
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("Registering user: username={}, email={}", request.getUsername(), request.getEmail());
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("Registration rejected due to duplicate username: {}", request.getUsername());
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration rejected due to duplicate email: {}", request.getEmail());
            throw new BadRequestException("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: userId={}", savedUser.getId());
        activityLogService.log(savedUser.getId(), "USER", savedUser.getId(), "REGISTER", java.util.Map.of(
                "username", savedUser.getUsername(),
                "email", savedUser.getEmail()
        ));
        return buildAuthResponse(savedUser);
    }

    /**
     * 驗證帳號密碼並建立新的登入權杖。
     *
     * @param request 登入資料
     * @return 使用者資訊與權杖
     */
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt: username={}", request.getUsername());
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Login rejected due to inactive user: userId={}, username={}", user.getId(), user.getUsername());
            throw new UnauthorizedException("User is inactive");
        }

        log.info("Login successful: userId={}, username={}", user.getId(), user.getUsername());
        activityLogService.log(user.getId(), "USER", user.getId(), "LOGIN", java.util.Map.of(
                "username", user.getUsername()
        ));
        return buildAuthResponse(user);
    }

    /**
     * 驗證 refresh token 後換發新的 access token。
     *
     * @param request refresh token 資料
     * @return 新的權杖資訊
     */
    @Override
    @Transactional
    public AuthTokenResponse refresh(RefreshRequest request) {
        log.debug("Refreshing access token");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (refreshToken.isRevokedFlag() || refreshToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            log.warn("Refresh token rejected: tokenId={}, revoked={}, expiredAt={}",
                    refreshToken.getId(), refreshToken.isRevokedFlag(), refreshToken.getExpiredAt());
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        User user = refreshToken.getUser();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        log.info("Access token refreshed successfully: userId={}, tokenId={}", user.getId(), refreshToken.getId());

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * 取得目前登入使用者的個人資訊摘要。
     *
     * @return 使用者摘要資訊
     * @throws UnauthorizedException 若使用者不存在
     */
    @Override
    @Transactional(readOnly = true)
    public UserSummaryResponse getCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        log.debug("Loading current user profile: userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        log.debug("Current user profile loaded: username={}", user.getUsername());
        return toUserSummary(user);
    }

    /**
     * 變更目前登入使用者的密碼。
     *
     * @param request 包含舊密碼與新密碼的請求
     * @throws UnauthorizedException 若使用者不存在
     * @throws BadRequestException 若舊密碼驗證失敗
     */
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("Changing password for userId={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("Password change rejected due to invalid current password: userId={}", userId);
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("Password changed successfully: userId={}", userId);

        activityLogService.log(userId, "USER", userId, "UPDATE", Map.of("changePassword", true));
    }

    /**
     * 註銷 refresh token，並視情況將 access token 加入黑名單。
     *
     * @param request 登出資料
     */
    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        log.info("Logout request received");
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        refreshToken.setRevokedFlag(true);
        refreshTokenRepository.save(refreshToken);

        if (request.getAccessToken() != null && !request.getAccessToken().isBlank()) {
            authTokenBlacklistService.blacklist(request.getAccessToken(), jwtService.getAccessTokenExpirationSeconds());
        }

        log.info("Logout completed: userId={}, tokenId={}",
                refreshToken.getUser() != null ? refreshToken.getUser().getId() : null,
                refreshToken.getId());
        activityLogService.log(
                refreshToken.getUser() != null ? refreshToken.getUser().getId() : null,
                "AUTH",
                refreshToken.getId(),
                "LOGOUT",
                java.util.Map.of("refreshTokenRevoked", true)
        );
    }

    /**
     * 建立包含使用者摘要與新權杖的登入回應，並持久化 refresh token。
     *
     * @param user 使用者實體
     * @return 登入回應資料
     */
    private AuthResponse buildAuthResponse(User user) {
        log.debug("Building auth response: userId={}", user.getId());
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshTokenValue = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiredAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationSeconds()))
                .revokedFlag(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        log.debug("Refresh token persisted: userId={}, tokenId={}", user.getId(), refreshToken.getId());

        return AuthResponse.builder()
                .user(toUserSummary(user))
                .tokens(AuthTokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshTokenValue)
                        .tokenType("Bearer")
                        .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                        .build())
                .build();
    }

    /**
     * 將使用者實體轉換為摘要回應物件。
     *
     * @param user 使用者實體
     * @return 使用者摘要資訊
     */
    private UserSummaryResponse toUserSummary(User user) {
        log.trace("Converting user entity to summary: userId={}, username={}", user.getId(), user.getUsername());
        return UserSummaryResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }
}
