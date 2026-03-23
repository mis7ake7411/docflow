package com.docflow.auth.controller;

import com.docflow.auth.dto.AuthResponse;
import com.docflow.auth.dto.AuthTokenResponse;
import com.docflow.auth.dto.ChangePasswordRequest;
import com.docflow.auth.dto.LoginRequest;
import com.docflow.auth.dto.LogoutRequest;
import com.docflow.auth.dto.RefreshRequest;
import com.docflow.auth.dto.RegisterRequest;
import com.docflow.auth.dto.UserSummaryResponse;
import com.docflow.auth.service.AuthService;
import com.docflow.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供註冊、登入、權杖刷新、目前登入者查詢與登出 API。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication and token management APIs")
public class AuthController {

    private final AuthService authService;

    /**
     * 註冊新使用者並回傳登入資訊。
     *
     * @param request 註冊資料
     * @return 使用者與權杖資訊
     */
    @Operation(summary = "Register user", description = "Create a new user account and return access/refresh tokens")
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request), "User registered successfully");
    }

    /**
     * 驗證帳號密碼並回傳登入資訊。
     *
     * @param request 登入資料
     * @return 使用者與權杖資訊
     */
    @Operation(summary = "Login", description = "Authenticate with username and password")
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request), "Login successful");
    }

    /**
     * 使用 refresh token 換發新的 access token。
     *
     * @param request refresh token 資料
     * @return 新的權杖資訊
     */
    @Operation(summary = "Refresh access token", description = "Issue a new access token using a valid refresh token")
    @PostMapping("/refresh")
    public ApiResponse<AuthTokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request), "Token refreshed successfully");
    }

    /**
     * 註銷 refresh token，並視情況將 access token 加入黑名單。
     *
     * @param request 登出資料
     * @return 成功回應
     */
    @Operation(summary = "Logout", description = "Revoke refresh token and optionally blacklist access token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return ApiResponse.success(null, "Logout successful");
    }

    @Operation(summary = "Get current user", description = "Return current authenticated user profile")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    public ApiResponse<UserSummaryResponse> me() {
        return ApiResponse.success(authService.getCurrentUser());
    }

    @Operation(summary = "Change password", description = "Update current user's password")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ApiResponse.success(null, "Password updated successfully");
    }
}
