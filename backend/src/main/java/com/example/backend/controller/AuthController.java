package com.example.backend.controller;

import com.example.backend.model.LoginRequest;
import com.example.backend.service.EmailService;
import com.example.backend.service.OtpService;
import com.example.backend.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final OtpService otpService;
    private final EmailService emailService;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    public AuthController(UserService userService, OtpService otpService, EmailService emailService) {
        this.userService = userService;
        this.otpService = otpService;
        this.emailService = emailService;
    }

    @PostMapping("/user-login")
    public CompletableFuture<ResponseEntity<Map<String, String>>> login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        logger.info("Login attempt for user: {}", loginRequest.getUsername());
        return userService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword())
            .thenCompose(user -> {
                if (user != null) {
                    logger.info("User authenticated successfully: {}", user.getUserName());
                    
                    HttpSession session = request.getSession(true);

                    session.setAttribute("username", user.getUserName());
                    
                    return otpService.generateOTP(user.getUserName())
                        .thenCompose(otp -> emailService.sendOtpEmail(user.getEmail(), otp))
                        .thenApply(emailSent -> {
                            if (emailSent) {
                                return ResponseEntity.ok(Map.of("message", "OTP sent successfully", "redirect", "/otp/verify"));
                            } else {
                                logger.error("Failed to send OTP for user: {}", user.getUserName());
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to send OTP"));
                            }
                        });
                } else {
                    logger.warn("Authentication failed for user: {}", loginRequest.getUsername());
                    return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials")));
                }
            })
            .exceptionally(e -> {
                logger.error("Unexpected error during login for user: {}", loginRequest.getUsername(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred"));
            });
    }


}
