package com.restaurant.user.command;

import com.restaurant.user.domain.UserProfile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateUserProfileCommand {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    @NotNull(message = "Profile cannot be null")
    @Valid
    private UserProfile profile;
    
    // Default constructor
    public UpdateUserProfileCommand() {
    }
    
    // Constructor
    public UpdateUserProfileCommand(String userId, UserProfile profile) {
        this.userId = userId;
        this.profile = profile;
    }
    
    // Getters and setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public UserProfile getProfile() {
        return profile;
    }
    
    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }
    
    @Override
    public String toString() {
        return "UpdateUserProfileCommand{" +
                "userId='" + userId + '\'' +
                ", profile=" + profile +
                '}';
    }
}