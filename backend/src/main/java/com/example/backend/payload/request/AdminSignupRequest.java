package com.example.backend.payload.request;

import lombok.Data;

@Data
public class AdminSignupRequest {

    private String adminName;
    private String email;
    private String password;
    private String firstName;
    private String lastName;

}
