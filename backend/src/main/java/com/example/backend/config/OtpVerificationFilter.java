package com.example.backend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class OtpVerificationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        System.out.println("OtpVerificationFilter: Processing request for " + path);

        if (path.startsWith("/auth/") || path.startsWith("/otp/")) {
            filterChain.doFilter(request, response);
            return;
        }

        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = request.getSession(false);

        if (auth == null || !auth.isAuthenticated() || session == null) {
            System.out.println("OtpVerificationFilter: User not authenticated");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"User not authenticated\", \"redirect\": \"/auth/login\"}");
            return;
        }
    
        Boolean otpVerified = (Boolean) session.getAttribute("otpVerified");
        System.out.println("OtpVerificationFilter: OTP verified = " + otpVerified);
        if (otpVerified == null || !otpVerified) {
            System.out.println("OtpVerificationFilter: OTP not verified, sending 401 Unauthorized");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"OTP verification required\", \"redirect\": \"/otp/verify\"}");
            return;
        }
    
        filterChain.doFilter(request, response);
    }
}
