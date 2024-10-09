package com.example.backend.controller;

import java.util.concurrent.CompletableFuture;
import java.util.Collections;
import java.util.Map;

import com.example.backend.service.EmailService;
import com.example.backend.service.OtpService;
import com.example.backend.service.UserService;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public CompletableFuture<ResponseEntity<?>> verifyOtp(@RequestBody Map<String, String> payload, HttpSession session) {
        String otp = payload.get("otp");
        String username = (String) session.getAttribute("username");
        
        if (username == null || otp == null || otp.isEmpty()) {
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(Map.of("error", "Invalid request")));
        }
        
        System.out.println("Verifying OTP for user: " + username);
        
        return otpService.validateOTP(username, otp)
            .thenApply(isValid -> {
                if (isValid) {
                    session.setAttribute("otpVerified", true);

                    Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        username, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                    );
                    SecurityContextHolder.getContext().setAuthentication(newAuth);
                    session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
                    
                    System.out.println("OTP verified successfully for user: " + username);
                    return ResponseEntity.ok().body(Map.of("message", "OTP verified successfully", "redirect", "/users/home"));
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