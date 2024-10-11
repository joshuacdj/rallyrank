package com.example.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.dto.LoginUserDto;
import com.example.backend.dto.RegisterUserDto;
import com.example.backend.dto.VerifyUserDto;
import com.example.backend.exception.UserNotEnabledException;
import com.example.backend.exception.UserNotFoundException;
import com.example.backend.model.User;
import com.example.backend.responses.LoginResponse;
import com.example.backend.security.UserPrincipal;
import com.example.backend.service.AuthenticationService;
import com.example.backend.service.JwtService;

@RestController
@RequestMapping("/auth")
public class AuthController {
    // for debugging
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationService authenticationService;
    private final JwtService jwtService;

    @Autowired
    public AuthController(AuthenticationService authenticationService, JwtService jwtService) {
        this.authenticationService = authenticationService;
        this.jwtService = jwtService;
    }

    @PostMapping("/user-signup")
    public ResponseEntity<User> signup(@RequestBody RegisterUserDto registerUserDto) {
        User registeredUser = authenticationService.signup(registerUserDto);
        return ResponseEntity.ok(registeredUser);
    
    }

    @PostMapping("/user-login")
    public ResponseEntity<?> login(@RequestBody LoginUserDto loginUserDto) {
        try {
            UserPrincipal authenticatedUser = authenticationService.authenticate(loginUserDto);
            String jwtToken = jwtService.generateToken(authenticatedUser);
            LoginResponse loginResponse = new LoginResponse(jwtToken, jwtService.getJwtExpiration());
            return ResponseEntity.ok(loginResponse);
        } catch (UserNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (UserNotEnabledException e) {
            logger.error("User not enabled: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Your account is not enabled. Please check your email to enable your account.");
        } catch (Exception e) {
            logger.error("Error occurred during login", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred during login");
        }
    }

    @PostMapping("/user-verify")
    public ResponseEntity<?> verifyUser(@RequestBody VerifyUserDto verifyUserDto) {
        try {
            authenticationService.verifyUser(verifyUserDto);
            return ResponseEntity.ok("User verified successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Invalid verification code");
        }
    }

    @PostMapping("/user-resend")
    public ResponseEntity<?> resendUserVerificationCode(@RequestBody String email) {
        try {
            authenticationService.resendVerificationCode(email);
            return ResponseEntity.ok("Verification code resent successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Failed to resend verification code");
        }
    }
}
