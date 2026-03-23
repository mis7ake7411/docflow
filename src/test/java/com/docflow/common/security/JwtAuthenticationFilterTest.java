package com.docflow.common.security;

import com.docflow.auth.service.AuthTokenBlacklistService;
import com.docflow.user.entity.User;
import com.docflow.user.entity.UserRole;
import com.docflow.user.entity.UserStatus;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void inactiveUserShouldNotAuthenticate() throws Exception {
        JwtService jwtService = Mockito.mock(JwtService.class);
        AuthTokenBlacklistService blacklist = Mockito.mock(AuthTokenBlacklistService.class);
        DocflowUserDetailsService userDetailsService = Mockito.mock(DocflowUserDetailsService.class);

        User inactive = User.builder()
                .id(1L)
                .username("alice")
                .passwordHash("hash")
                .role(UserRole.USER)
                .status(UserStatus.INACTIVE)
                .mustChangePassword(false)
                .build();
        DocflowUserPrincipal principal = new DocflowUserPrincipal(inactive);

        Mockito.when(jwtService.extractUsername("token")).thenReturn("alice");
        Mockito.when(jwtService.isTokenValid("token", "alice")).thenReturn(true);
        Mockito.when(userDetailsService.loadUserByUsername("alice")).thenReturn(principal);

        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userDetailsService, blacklist);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = Mockito.mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
