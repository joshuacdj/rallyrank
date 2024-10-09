package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for verifying a user's email address.
 */

@Getter
@Setter
public class VerifyUserDto {
    private String username;
    private String verificationCode;
}
