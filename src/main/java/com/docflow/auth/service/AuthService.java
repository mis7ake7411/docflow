package com.docflow.auth.service;

import com.docflow.auth.dto.AuthResponse;
import com.docflow.auth.dto.AuthTokenResponse;
import com.docflow.auth.dto.ChangePasswordRequest;
import com.docflow.auth.dto.LoginRequest;
import com.docflow.auth.dto.LogoutRequest;
import com.docflow.auth.dto.RefreshRequest;
import com.docflow.auth.dto.RegisterRequest;
import com.docflow.auth.dto.UserSummaryResponse;

/**
 * 提供註冊、登入、權杖刷新、目前登入者查詢與登出功能。
 */
public interface AuthService {

    /**
     * 註冊新使用者並建立登入權杖。
     *
     * @param request 註冊資料
     * @return 使用者與權杖資訊
     */
    AuthResponse register(RegisterRequest request);

    /**
     * 驗證帳號密碼並建立登入權杖。
     *
     * @param request 登入資料
     * @return 使用者與權杖資訊
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
     * 取得目前已登入使用者資料。
     *
     * @return 目前登入者摘要
     */
    UserSummaryResponse getCurrentUser();

    /**
     * 更新目前使用者密碼
     *
     * @param request 變更密碼請求
     */
    void changePassword(ChangePasswordRequest request);

    /**
     * 註銷 refresh token，並視情況將 access token 加入黑名單。
     *
     * @param request 登出資料
     */
    void logout(LogoutRequest request);
}
