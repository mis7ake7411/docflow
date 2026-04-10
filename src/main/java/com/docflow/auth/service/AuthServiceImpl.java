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
 * {@link AuthService} ?勯?瑷浣滐?璨犺铂浣跨敤?呰ɑ?娿€佺櫥?ヨ?娆婃??熷懡?辨?绠＄???
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
     * 瑷诲??颁娇?ㄨ€呬甫寤虹??濆??诲叆娆婃???
     *
     * @param request 瑷诲?璩囨?
     * @return 浣跨敤?呰?瑷婅?娆婃?
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
     * 椹楄?甯宠?瀵嗙⒓涓﹀缓绔嬫柊?勭櫥?ユ??栥€?
     *
     * @param request ?诲叆璩囨?
     * @return 浣跨敤?呰?瑷婅?娆婃?
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
     * 椹楄? refresh token 寰屾??兼柊??access token??
     *
     * @param request refresh token 璩囨?
     * @return ?扮?娆婃?璩囪?
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
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("Refresh rejected due to inactive user: userId={}, tokenId={}", user.getId(), refreshToken.getId());
            throw new UnauthorizedException("User is inactive");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String newRefreshTokenValue = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());

        refreshToken.setToken(newRefreshTokenValue);
        refreshToken.setExpiredAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationSeconds()));
        refreshToken.setRevokedFlag(false);
        refreshTokenRepository.save(refreshToken);

        log.info("Access token refreshed successfully: userId={}, tokenId={}", user.getId(), refreshToken.getId());

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }

    /**
     * ?栧????诲叆浣跨敤?呯??嬩汉璩囪??樿???
     *
     * @return 浣跨敤?呮?瑕佽?瑷?
     * @throws UnauthorizedException ?ヤ娇?ㄨ€呬?瀛樺湪
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
     * 璁婃洿???诲叆浣跨敤?呯?瀵嗙⒓??
     *
     * @param request ?呭惈?婂?纰艰??板?纰肩?璜嬫?
     * @throws UnauthorizedException ?ヤ娇?ㄨ€呬?瀛樺湪
     * @throws BadRequestException ?ヨ?瀵嗙⒓椹楄?澶辨?
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
     * 瑷婚姺 refresh token锛屼甫瑕栨?娉佸? access token ?犲叆榛戝??€?
     *
     * @param request ?诲嚭璩囨?
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
     * 寤虹??呭惈浣跨敤?呮?瑕佽??版??栫??诲叆?炴?锛屼甫?佷???refresh token??
     *
     * @param user 浣跨敤?呭楂?
     * @return ?诲叆?炴?璩囨?
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
     * 灏囦娇?ㄨ€呭楂旇??涚偤?樿??炴??╀欢??
     *
     * @param user 浣跨敤?呭楂?
     * @return 浣跨敤?呮?瑕佽?瑷?
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
