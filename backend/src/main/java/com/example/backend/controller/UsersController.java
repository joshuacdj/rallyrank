package com.example.backend.controller;
import com.example.backend.model.User;
import com.example.backend.service.UserService;

import lombok.AllArgsConstructor;

import com.example.backend.exception.UserNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@RestController // This annotation marks the class as a RESTful web service controller
@RequestMapping("/users") // This annotation maps the class to the users endpoint
// This annotation generates a constructor for the class with final fields
@AllArgsConstructor
public class UsersController {

    private final UserService userService;
   
    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

    // Async health check endpoint to get all the users
    @GetMapping
    public CompletableFuture<ResponseEntity<?>> getAllUsers() {
        return userService.getAllUsers()
            .<ResponseEntity<?>>thenApply(users -> {
                logger.info("Successfully fetched {} users!", users.size());
                return ResponseEntity.ok(users);
            })
            .exceptionally(e -> {
                logger.error("Error getting all users", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "An unexpected error occurred while fetching users!"));
            });
    }

    /**
     * Asynchronously checks the availability of a username and/or email.
     * 
     * This method checks if username and/or email are available for sign up purposes.
     * 
     * @param username the username to check for availability (optional)
     * @param email the email to check for availability (optional)
     * @return a CompletableFuture containing a response entity with the availability status and message.
     */
    @GetMapping("/signup/check-availability")
    public CompletableFuture<ResponseEntity<?>> checkAvailability(@RequestParam(required = false) String username, @RequestParam(required = false) String email) {
        Map<String, Object> response = new HashMap<>();

        if (username == null && email == null) {
            response.put("message", "Either username or email must be provided!");
            return CompletableFuture.completedFuture(ResponseEntity.badRequest().body(response));
        }

        // Create futures for checking username and email availability
        CompletableFuture<Boolean> usernameFuture = username != null ? userService.checkIfUsernameExists(username) : CompletableFuture.completedFuture(null);
        CompletableFuture<Boolean> emailFuture = email != null ? userService.checkIfEmailExists(email) : CompletableFuture.completedFuture(null);

        return CompletableFuture.allOf(usernameFuture, emailFuture)
            .<ResponseEntity<?>>thenApply(v -> {
                Boolean usernameExists = usernameFuture.join();
                Boolean emailExists = emailFuture.join();

                StringBuilder message = new StringBuilder();
                boolean hasError = false;

                // Check if username is provided and if it exists
                if (username == null) {
                    message.append("Username must be provided. ");
                    hasError = true;
                } else {
                    response.put("usernameAvailable", !usernameExists);
                    if (usernameExists) {
                        message.append("Username is already taken. ");
                    }
                }

                // Check if email is provided and if it exists
                if (email == null) {
                    message.append("Email must be provided. ");
                    hasError = true;
                } else {
                    response.put("emailAvailable", !emailExists);
                    if (emailExists) {
                        message.append("Email is already in use. ");
                    }
                }

                // If no errors and both username and email are provided, set both as available
                if (!hasError && message.length() == 0) {
                    if (username != null && email != null) {
                        message.append("Username and email are available.");
                    } else if (username != null) {
                        message.append("Username is available.");
                    } else {
                        message.append("Email is available.");
                    }
                }

                response.put("message", message.toString().trim());

                if (hasError) {
                    return ResponseEntity.badRequest().body(response);
                } else {
                    return ResponseEntity.ok(response);
                }
            })
            .exceptionally(e -> {
                Throwable cause = e.getCause();
                response.put("message", cause.getMessage());
                return ResponseEntity.badRequest().body(response);
            });
    }

    /**
     * Asynchronously creates a new user and updates the user details in the
     * database.
     *
     * This method handles the HTTP POST request to the "/signup" endpoint. 
     * It validates the user data and checks for any existing users with the same email or username. 
     * If validation fails, it returns a bad request response with the validation error messages. 
     * If the user is created successfully, it returns a success response with the created user details.
     *
     * @param user the User object containing the details of the user to be created.
     * @return a CompletableFuture containing a ResponseEntity. If the user is
     *         created successfully,
     *         it returns a ResponseEntity with HTTP status 200 (OK) and the created
     *         user. If there are
     *         validation errors or other issues, it returns a ResponseEntity with
     *         HTTP status 400 (Bad Request)
     *         and the corresponding error message.
     */
    @PostMapping("/signup")
    public CompletableFuture<ResponseEntity<?>> createUser(@RequestBody User user) {
        return userService.createUser(user)
                .<ResponseEntity<?>>thenApply(createdUser -> {
                    logger.info("User created successfully: {}", createdUser.getUsername());
                    return ResponseEntity.ok(createdUser);
                })
                .exceptionally(e -> {
                    Throwable cause = e.getCause();
                    if (cause instanceof IllegalArgumentException) {
                        logger.error("Validation errors during user creation: {}", cause.getMessage());
                        return ResponseEntity.badRequest().body(Map.of("error", cause.getMessage()));
                    } else {
                        logger.error("Error creating user: {}", cause.getMessage(), cause);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(Map.of("error", "An unexpected error occurred while creating the user!"));
                    }
                });
    }

    /**
     * Retrieves the user profile based on the user name
     * @param username the username of the user to retrieve, must not be null or empty
     * @return the user object associated with the specified username
     * @throws UserNotFoundException if no user with the username is found in the database
     * @throws RuntimeException if there is an unexpected error during the retrieval process
     */
    @GetMapping("/{username}")
    public ResponseEntity<?> getUserProfile(@PathVariable String username) {
        try {
            User user = userService.getUserByUsername(username);
            logger.info("User profile retrieved successfully: {}", username);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            logger.error("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving user profile: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred while retrieving the user profile!"));
        }
    }

    /**
     * Updates the user details based on the user name
     * @param username the username of the user to update, must not be null or empty
     * @param newUserDetails the new details of the user to be updated
     * @return the updated user object
     * @throws UserNotFoundException if no user with the username is found in the database
     * @throws RuntimeException if there is an unexpected error during the update process
     */
    // @PutMapping("/{username}/update")
    // public CompletableFuture<ResponseEntity<?>> updateUser(@PathVariable String username, @RequestBody User newUserDetails) {
    //     return userService.updateUser(username, newUserDetails)
    //         .<ResponseEntity<?>>thenApply(updatedUser -> {
    //             logger.info("User updated successfully: {}", username);
    //             return ResponseEntity.ok(updatedUser);
    //         })
    //         .exceptionally(e -> {
    //             Throwable cause = e.getCause();
    //             if (cause instanceof IllegalArgumentException) {
    //                 logger.error("Validation errors during user update: {}", cause.getMessage());
    //                 return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", cause.getMessage()));
    //             } else if (cause instanceof UserNotFoundException) {
    //                 logger.error("User not found: {}", username);
    //                 return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", cause.getMessage()));
    //             } else {
    //                 logger.error("Unexpected error updating user: {}", cause.getMessage(), cause);
    //                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(cause.getMessage());
    //             }
    //         });
    // }

    // REMOVED CompletableFuture and added .get()
    @PutMapping("/{username}/update")
    public ResponseEntity<?> updateUser(@PathVariable String username, @RequestBody User newUserDetails) {
        try {
            User updatedUser = userService.updateUser(username, newUserDetails).get(); // Wait for the CompletableFuture to complete
            logger.info("User updated successfully: {}", username);
            return ResponseEntity.ok(updatedUser);
        } catch (InterruptedException | ExecutionException e) {
            logger.error("Error updating user: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while updating the user");
        } catch (Exception e) {
            if (e.getCause() instanceof IllegalArgumentException) {
                logger.error("Validation errors during user update: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
            } else if (e.getCause() instanceof UserNotFoundException) {
                logger.error("User not found: {}", username);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
            } else {
                logger.error("Unexpected error updating user: {}", e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
            }
        }
    }

    /**
     * Updates the availability of a user based on the user name
     * @param username the username of the user to update, must not be null or empty
     * @param availability the new availability status of the user
     * @return the updated user object
     * @throws UserNotFoundException if no user with the username is found in the database
     * @throws RuntimeException if there is an unexpected error during the update process
     */
    @PutMapping("/{username}/update-availability")
    public ResponseEntity<?> updateUserAvailability(@PathVariable String username, @RequestParam Boolean availability) {
        try {
            User user = userService.updateUserAvailability(username, availability);
            logger.info("User availability updated successfully: {}", username);
            return ResponseEntity.ok(user);
        } catch (UserNotFoundException e) {
            logger.error("User not found: {}", username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating user availability: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred while updating the user availability!"));
        }
    }

    // @PostMapping("/login")
    // public CompletableFuture<ResponseEntity<Map<String, String>>> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
    //     logger.info("Login attempt for user: {}", loginRequest.getUsername());
    //     return userService.authenticateUser(loginRequest.getUsername(), loginRequest.getPassword())
    //         .thenCompose(user -> {
    //             if (user != null) {
    //                 logger.info("User authenticated successfully: {}", user.getUsername());
    //                 return otpService.generateOTP(user.getUsername())
    //                     .thenCompose(otp -> emailService.sendOtpEmail(user.getEmail(), otp))
    //                     .thenApply(emailSent -> {
    //                         if (emailSent) {
    //                             session.setAttribute("USER_ID", user.getId());
    //                             session.setAttribute("needOtpVerification", true);
    //                             return ResponseEntity.ok(Map.of("message", "OTP sent successfully", "redirect", "/otp/verify"));
    //                         } else {
    //                             logger.error("Failed to send OTP for user: {}", user.getUsername());
    //                             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to send OTP"));
    //                         }
    //                     });
    //             } else {
    //                 logger.warn("Authentication failed for user: {}", loginRequest.getUsername());
    //                 return CompletableFuture.completedFuture(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid credentials")));
    //             }
    //         })
    //         .exceptionally(e -> {
    //             logger.error("Unexpected error during login for user: {}", loginRequest.getUsername(), e);
    //             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "An unexpected error occurred"));
    //         });
    // }
}
