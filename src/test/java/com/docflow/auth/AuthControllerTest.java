package com.docflow.auth;

import com.docflow.auth.controller.AuthController;
import com.docflow.test.TestSecurityConfig;
import com.docflow.auth.dto.*;
import com.docflow.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void registerShouldReturnSuccessResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .user(UserSummaryResponse.builder()
                        .id(1L)
                        .username("alice")
                        .email("alice@example.com")
                        .role("USER")
                        .status("ACTIVE")
                        .build())
                .tokens(AuthTokenResponse.builder()
                        .accessToken("access-token")
                        .refreshToken("refresh-token")
                        .tokenType("Bearer")
                        .expiresIn(3600)
                        .build())
                .build();

        Mockito.when(authService.register(Mockito.any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.username").value("alice"));
    }

    @Test
    void registerShouldReturnMustChangePasswordFlag() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        AuthResponse response = AuthResponse.builder()
                .user(UserSummaryResponse.builder()
                        .id(1L)
                        .username("alice")
                        .email("alice@example.com")
                        .role("USER")
                        .status("ACTIVE")
                        .mustChangePassword(false)
                        .build())
                .tokens(AuthTokenResponse.builder()
                        .accessToken("access-token")
                        .refreshToken("refresh-token")
                        .tokenType("Bearer")
                        .expiresIn(3600)
                        .build())
                .build();

        Mockito.when(authService.register(Mockito.any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.mustChangePassword").value(false));
    }

    @Test
    @WithMockUser
    void logoutShouldReturnSuccessResponse() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
