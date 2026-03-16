package com.docflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {

    private UserSummaryResponse user;
    private AuthTokenResponse tokens;
}
