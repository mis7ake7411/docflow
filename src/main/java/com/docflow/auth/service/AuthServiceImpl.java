package com.docflow.auth.service;

import com.docflow.auth.dto.*;
import com.docflow.auth.entity.RefreshToken;
import com.docflow.auth.repository.RefreshTokenRepository;
import com.docflow.common.exception.BadRequestException;
import com.docflow.common.exception.UnauthorizedException;
import com.docflow.common.security.JwtService;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import com.docflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
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
        return buildAuthResponse(savedUser);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthTokenResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        if (refreshToken.isRevokedFlag() || refreshToken.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }

        User user = refreshToken.getUser();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());

        return AuthTokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token not found"));

        refreshToken.setRevokedFlag(true);
        refreshTokenRepository.save(refreshToken);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshTokenValue = jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiredAt(LocalDateTime.now().plusSeconds(jwtService.getRefreshTokenExpirationSeconds()))
                .revokedFlag(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .user(UserSummaryResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .status(user.getStatus().name())
                        .build())
                .tokens(AuthTokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshTokenValue)
                        .tokenType("Bearer")
                        .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                        .build())
                .build();
    }
}
