package com.docflow.auth.service;

import com.docflow.auth.dto.*;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthTokenResponse refresh(RefreshRequest request);

    void logout(LogoutRequest request);
}
