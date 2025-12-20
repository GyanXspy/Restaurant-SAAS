package com.restaurant.user.domain;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data
@NoArgsConstructor
@EqualsAndHashCode(of = "userId")
@ToString(of = {"userId", "email", "status", "createdAt"})
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true, name = "idx_user_id_unique")
    private String userId;
    
    @Indexed(unique = true, name = "idx_email_unique")
    private String email;
    
    private UserProfile profile;
    
    private UserStatus status;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Constructor for creating new user
    public User(String userId, String email, UserProfile profile) {
        this.userId = userId;
        this.email = email;
        this.profile = profile;
        this.status = UserStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        validateUser();
    }
    
    // Business logic methods
    public void updateProfile(UserProfile newProfile) {
        if (newProfile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }
        this.profile = newProfile;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateEmail(String newEmail) {
        if (newEmail == null || newEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!isValidEmail(newEmail)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        this.email = newEmail;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void deactivate() {
        this.status = UserStatus.INACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public void activate() {
        this.status = UserStatus.ACTIVE;
        this.updatedAt = LocalDateTime.now();
    }
    
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
    
    // Validation methods
    private void validateUser() {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null or empty");
        }
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Profile cannot be null");
        }
    }
    
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}