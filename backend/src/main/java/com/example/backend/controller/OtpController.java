package com.example.backend.controller;

import java.util.concurrent.CompletableFuture;
import java.util.Map;

import com.example.backend.service.EmailService;
import com.example.backend.service.OtpService;
import com.example.backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/otp")
public class OtpController {

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private UserService userService;

    @GetMapping("/verify")
    public CompletableFuture<ResponseEntity<?>> showOtpVerificationPage() {
        return CompletableFuture.completedFuture(
            ResponseEntity.ok().body(Map.of("message", "Please enter your OTP"))
        );
    }

    @PostMapping("/verify")
    public CompletableFuture<ResponseEntity<?>> verifyOtp(@RequestBody Map<String, String> payload, Authentication authentication) {
        String username = authentication.getName();
        String otp = payload.get("otp");
        System.out.println("Verifying OTP for user: " + username);
        
        if (otp == null || otp.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of("error", "OTP is required")));
        }
        
        return otpService.validateOTP(username, otp)
            .thenApply(isValid -> {
                if (isValid) {
                    System.out.println("OTP verified successfully for user: " + username);
                    if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                        return ResponseEntity.ok().body(Map.of("message", "OTP verified successfully", "redirect", "/admins/home"));
                    } else {
                        return ResponseEntity.ok().body(Map.of("message", "OTP verified successfully", "redirect", "/users/home"));
                    }
                } else {
                    System.out.println("OTP verification failed for user: " + username);
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid OTP entered. Please try again."));
                }
            });
    }

    @PostMapping("/send")
    public CompletableFuture<ResponseEntity<?>> sendOtp(Authentication authentication) {
        String username = authentication.getName();
        String email = userService.getUserByUsername(username).getEmail();
        
        return otpService.generateOTP(username)
            .thenCompose(otp -> emailService.sendOtpEmail(email, otp))
            .thenApply(emailSent -> {
                if (emailSent) {
                    return ResponseEntity.ok().body(Map.of("message", "OTP sent successfully. It will expire in 5 minutes."));
                } else {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to send OTP. Please try again."));
                }
            });
    }
}