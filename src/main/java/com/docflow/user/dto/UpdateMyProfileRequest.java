package com.docflow.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMyProfileRequest {

    @Email(message = "Email format is invalid")
    @NotBlank(message = "Email is required")
    private String email;
}

