package com.example.backend.controller;

import com.example.backend.model.User;
import com.example.backend.service.UserService;
import com.example.backend.service.AdminService;
import com.example.backend.config.jwt.JwtUtils;
import com.example.backend.service.UserDetailsImpl;
import com.example.backend.payload.request.LoginRequest;
import com.example.backend.payload.request.UserSignupRequest;
import com.example.backend.payload.response.JwtResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
public class AuthController {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserService userService;

    @Autowired
    AdminService adminService;

    @Autowired
    PasswordEncoder encoder;

    @Autowired
    JwtUtils jwtUtils;

    @PostMapping("/users/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) { //loginRequest is just a class that has email, username and password 
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/admins/login")
    public ResponseEntity<?> authenticateAdmin(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        if (!roles.contains("ROLE_ADMIN")) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied. User is not an admin."));
        }

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles));
    }

    @PostMapping("/users/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserSignupRequest signUpRequest) {

        
        if (userService.checkIfUserNameExists(signUpRequest.getUserName())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Username is already taken!"));
        }

        if (userService.checkIfEmailExists(signUpRequest.getEmail())) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", "Email is already in use!"));
        }

        // Create new user's account

        User user = new User();
        user.setUserName(signUpRequest.getUserName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(encoder.encode(signUpRequest.getPassword()));
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());   
        user.setPhoneNumber(signUpRequest.getPhoneNumber());
        user.setGender(signUpRequest.getGender());
        user.setDateOfBirth(signUpRequest.getDateOfBirth());
        user.setElo(signUpRequest.getElo());
        user.setAge(signUpRequest.getAge());
        user.setMedicalInformation(signUpRequest.getMedicalInformation());

        userService.createUser(user);

        return ResponseEntity.ok(Map.of("message", "User registered successfully!"));
    }

    // TODO
    // @PostMapping("/admins/signup")
    // public ResponseEntity<?> registerAdmin(@Valid @RequestBody AdminSignupRequest signUpRequest) {
    //     if (adminService.getAdminById(signUpRequest.getAdminName()).isPresent()) {
    //         return ResponseEntity
    //                 .badRequest()
    //                 .body(Map.of("error", "Admin name is already taken!"));
    //     }

    //     // Create new admin's account
    //     Admin admin = new Admin();
    //     admin.setAdminName(signUpRequest.getAdminName());
    //     admin.setEmail(signUpRequest.getEmail());
    //     admin.setPassword(encoder.encode(signUpRequest.getPassword()));

    //     adminService.createAdmin(admin);

    //     return ResponseEntity.ok(Map.of("message", "Admin registered successfully!"));
    // }
}
