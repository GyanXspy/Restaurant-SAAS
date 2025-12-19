package com.restaurant.user.command;

import com.restaurant.user.domain.UserProfile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateUserCommand {

    @NotBlank(message = "User ID cannot be blank")
    private String userId;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    private String email;

    @NotNull(message = "Profile cannot be null")
    @Valid
    private UserProfile profile;

    // Default constructor
    public CreateUserCommand() {
    }

    // Constructor
    public CreateUserCommand(String userId, String email, UserProfile profile) {
        this.userId = userId;
        this.email = email;
        this.profile = profile;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public void setProfile(UserProfile profile) {
        this.profile = profile;
    }

    @Override
    public String toString() {
        return "CreateUserCommand{"
                + "userId='" + userId + '\''
                + ", email='" + email + '\''
                + ", profile=" + profile
                + '}';
    }
}
