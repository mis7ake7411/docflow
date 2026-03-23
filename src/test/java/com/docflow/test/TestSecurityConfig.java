package com.docflow.test;

import com.docflow.auth.service.AuthTokenBlacklistService;
import com.docflow.common.security.DocflowUserDetailsService;
import com.docflow.common.security.JwtService;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    JwtService jwtService() {
        return Mockito.mock(JwtService.class);
    }

    @Bean
    DocflowUserDetailsService docflowUserDetailsService() {
        return Mockito.mock(DocflowUserDetailsService.class);
    }

    @Bean
    AuthTokenBlacklistService authTokenBlacklistService() {
        return Mockito.mock(AuthTokenBlacklistService.class);
    }
}
