package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain for HTTP requests.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(requests -> requests
                .requestMatchers("/css/**", "/js/**", "/images/**", "/users/login", "/admins/login").permitAll()
                .requestMatchers("/admins/**").hasRole("ADMIN")
                .requestMatchers("/users/**").hasRole("USER")
                .anyRequest().authenticated())
            .logout(logout -> logout
                .permitAll())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/users/login", "/admins/login"));

        return http.build();
    }

    /**
     * Provides a custom authentication success handler.
     */

    /**
     * Provides a password encoder for secure password storage.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}






