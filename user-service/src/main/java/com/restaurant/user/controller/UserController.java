package com.restaurant.user.controller;

import com.restaurant.user.domain.User;
import com.restaurant.user.domain.UserProfile;
import com.restaurant.user.dto.CreateUserRequest;
import com.restaurant.user.dto.UserResponse;
import com.restaurant.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<String> createUser(@RequestBody CreateUserRequest request) {
        log.info("Received create user request for ID: {}", request.getUserId());
        
        try {
            String userId = userService.publishUserCreationEvent(
                request.getUserId(), 
                request.getEmail(), 
                request.getProfile()
            );
            
            return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("User creation event published. UserId: " + userId);
                
        } catch (Exception e) {
            log.error("Failed to publish user creation event: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to publish user creation event");
        }
    }
    
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable String userId) {
        return userService.getUserById(userId)
            .map(user -> ResponseEntity.ok(mapToResponse(user)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
    
    @GetMapping("/active")
    public ResponseEntity<List<UserResponse>> getActiveUsers() {
        List<UserResponse> users = userService.getActiveUsers().stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }
    
    @PutMapping("/{userId}/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable String userId,
            @RequestBody UserProfile profile) {
        
        try {
            User user = userService.updateUserProfile(userId, profile);
            return ResponseEntity.ok(mapToResponse(user));
        } catch (IllegalArgumentException e) {
            log.error("Failed to update profile: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<UserResponse> deactivateUser(@PathVariable String userId) {
        try {
            User user = userService.deactivateUser(userId);
            return ResponseEntity.ok(mapToResponse(user));
        } catch (IllegalArgumentException e) {
            log.error("Failed to deactivate user: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
    
    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUserId(user.getUserId());
        response.setEmail(user.getEmail());
        response.setProfile(user.getProfile());
        response.setStatus(user.getStatus());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }
}
