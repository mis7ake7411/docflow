package com.docflow.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class})
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PasswordChangeRequiredFilter passwordChangeRequiredFilter;
    private final DocflowUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login",
                                "/app",
                                "/app/**",
                                "/assets/**",
                                "/favicon.ico",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/me", "/api/auth/change-password", "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/users/me/**").hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/users/me/profile").hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/users/share-candidates").hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers("/api/users/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/documents/*/shares", "/api/documents/*/shares/**")
                        .hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/documents/*/shares")
                        .hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/documents/*/shares/*")
                        .hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/documents/*/shares/*")
                        .hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/documents/**", "/api/folders/**", "/api/stats/**", "/api/activities/**")
                        .hasAnyRole("USER", "ADMIN", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/api/documents/**", "/api/folders/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/documents/**", "/api/folders/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/documents/**", "/api/folders/**").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(passwordChangeRequiredFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
