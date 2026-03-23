package com.docflow.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CreateUserResponse {

    private UserListItemResponse user;
    private String tempPassword;
}
