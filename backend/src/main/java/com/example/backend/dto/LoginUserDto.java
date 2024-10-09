package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for logging in a user.
 */

@Getter
@Setter
public class LoginUserDto {
    private String username;
    private String password;
}
