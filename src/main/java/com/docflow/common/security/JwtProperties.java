package com.docflow.common.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "docflow.jwt")
public class JwtProperties {

    private String secret = "change-me-change-me-change-me-change-me";
    private long accessTokenExpirationSeconds = 3600;
    private long refreshTokenExpirationSeconds = 604800;
}
