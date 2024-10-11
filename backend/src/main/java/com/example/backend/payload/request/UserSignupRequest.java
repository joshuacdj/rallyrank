package com.example.backend.payload.request;

import java.time.LocalDate;
import com.example.backend.model.User.MedicalInformation;
import lombok.Data;

@Data
public class UserSignupRequest {

    private String userName;
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
