package com.docflow.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class UserSummaryResponse {

    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
}
