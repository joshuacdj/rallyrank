package com.example.backend.dto;

import java.time.LocalDate;

import com.example.backend.model.User.MedicalInformation;

import lombok.Data;

/**
 * DTO for registering a new user.
 */

@Data
public class RegisterUserDto {

    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private int elo = 400;
    private String gender;
    private LocalDate dateOfBirth;
    private MedicalInformation medicalInformation;
    private int age;
}
