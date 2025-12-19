package com.restaurant.user.command;

import jakarta.validation.constraints.NotBlank;

public class DeactivateUserCommand {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    private String reason;
    
    // Default constructor
    public DeactivateUserCommand() {
    }
    
    // Constructor
    public DeactivateUserCommand(String userId, String reason) {
        this.userId = userId;
        this.reason = reason;
    }
    
    // Getters and setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    @Override
    public String toString() {
        return "DeactivateUserCommand{" +
                "userId='" + userId + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}