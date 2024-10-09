package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for registering a new user.
 */

@Getter
@Setter
public class RegisterUserDto {
    private String username;
    private String email;
    private String password;
}
