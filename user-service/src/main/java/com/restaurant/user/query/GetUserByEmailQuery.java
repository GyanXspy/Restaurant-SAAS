package com.restaurant.user.query;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class GetUserByEmailQuery {
    
    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be valid")
    private String email;
    
    // Default constructor
    public GetUserByEmailQuery() {
    }
    
    // Constructor
    public GetUserByEmailQuery(String email) {
        this.email = email;
    }
    
    // Getters and setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    @Override
    public String toString() {
        return "GetUserByEmailQuery{" +
                "email='" + email + '\'' +
                '}';
    }
}