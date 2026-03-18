package com.docflow.auth.service;

import com.docflow.auth.dto.*;

/**
 * 提供認證相關操作，例如註冊、登入、刷新權杖與登出。
 */
public interface AuthService {

    /**
     * 註冊新使用者並回傳登入資訊。
     *
     * @param request 註冊資料
     * @return 使用者資訊與權杖
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 驗證帳號密碼並回傳登入資訊。
     *
     * @param request 登入資料
     * @return 使用者資訊與權杖
     */
    AuthResponse login(LoginRequest request);

    /**
     * 使用 refresh token 換發新的 access token。
     *
     * @param request refresh token 資料
     * @return 新的權杖資訊
     */
    AuthTokenResponse refresh(RefreshRequest request);

    /**
     * 註銷 refresh token，並視情況將 access token 加入黑名單。
     *
     * @param request 登出資料
     */
    void logout(LogoutRequest request);
}
