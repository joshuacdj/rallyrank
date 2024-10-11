package com.example.backend.service;

import com.example.backend.model.Admin;
import com.example.backend.model.User;
import com.example.backend.repository.AdminRepository;
import com.example.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    AdminRepository adminRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.info("UserDetailsServiceImpl: Attempting to load user: " + username);
        User user = userRepository.findByUserName(username).orElse(null);
        

        // If user is found, return the user details
        if (user != null) {
            return UserDetailsImpl.build(user);
        }

        // Else if admin is found, return the admin details
        Admin admin = adminRepository.findByAdminName(username).orElse(null);
        if (admin != null) {
            return UserDetailsImpl.build(admin);
        }

        // Else if both user and admin are not found, throw an exception
        throw new UsernameNotFoundException("User or Admin Not Found with username: " + username);
    }
}