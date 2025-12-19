package com.restaurant.user.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.user.command.CreateUserCommand;
import com.restaurant.user.command.DeactivateUserCommand;
import com.restaurant.user.command.UpdateUserProfileCommand;
import com.restaurant.user.command.UserCommandHandler;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/commands")
public class UserCommandController {

    private static final Logger logger = LoggerFactory.getLogger(UserCommandController.class);

    private final UserCommandHandler commandHandler;

    @Autowired
    public UserCommandController(UserCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }

    @PostMapping
    public ResponseEntity<CreateUserResponse> createUser(@Valid @RequestBody CreateUserCommand command) {
        logger.info("Received request to create user with ID: {}", command.getUserId());

        try {
            String userId = commandHandler.handle(command);
            CreateUserResponse response = new CreateUserResponse(userId, "User created successfully");

            logger.info("User created successfully with ID: {}", userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.error("Failed to create user: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new CreateUserResponse(null, e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error creating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CreateUserResponse(null, "Internal server error"));
        }
    }

    @PutMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse> updateUserProfile(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserProfileCommand command) {

        logger.info("Received request to update profile for user ID: {}", userId);

        // Ensure the userId in path matches the command
        command.setUserId(userId);

        try {
            commandHandler.handle(command);

            logger.info("User profile updated successfully for ID: {}", userId);
            return ResponseEntity.ok(new ApiResponse("User profile updated successfully"));

        } catch (IllegalArgumentException e) {
            logger.error("Failed to update user profile: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error updating user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Internal server error"));
        }
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<ApiResponse> deactivateUser(
            @PathVariable String userId,
            @RequestBody(required = false) DeactivateUserRequest request) {

        logger.info("Received request to deactivate user ID: {}", userId);

        String reason = request != null ? request.getReason() : "User requested deactivation";
        DeactivateUserCommand command = new DeactivateUserCommand(userId, reason);

        try {
            commandHandler.handle(command);

            logger.info("User deactivated successfully for ID: {}", userId);
            return ResponseEntity.ok(new ApiResponse("User deactivated successfully"));

        } catch (IllegalArgumentException e) {
            logger.error("Failed to deactivate user: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error deactivating user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse("Internal server error"));
        }
    }

    // Response DTOs
    public static class CreateUserResponse {

        private String userId;
        private String message;

        public CreateUserResponse(String userId, String message) {
            this.userId = userId;
            this.message = message;
        }

        public String getUserId() {
            return userId;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class ApiResponse {

        private String message;

        public ApiResponse(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class DeactivateUserRequest {

        private String reason;

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
