package com.restaurant.user.query;

import jakarta.validation.constraints.NotBlank;

public class GetUserQuery {
    
    @NotBlank(message = "User ID cannot be blank")
    private String userId;
    
    // Default constructor
    public GetUserQuery() {
    }
    
    // Constructor
    public GetUserQuery(String userId) {
        this.userId = userId;
    }
    
    // Getters and setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @Override
    public String toString() {
        return "GetUserQuery{" +
                "userId='" + userId + '\'' +
                '}';
    }
}