package com.restaurant.user.domain;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserProfile {
    
    private String firstName;
    private String lastName;
    private String phone;
    private List<Address> addresses = new ArrayList<>();
    
    // Constructor
    public UserProfile(String firstName, String lastName, String phone) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.addresses = new ArrayList<>();
        validateProfile();
    }
    
    // Business logic methods
    public void addAddress(Address address) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null");
        }
        this.addresses.add(address);
    }
    
    public void removeAddress(Address address) {
        this.addresses.remove(address);
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
    
    // Custom getter for addresses to return defensive copy
    public List<Address> getAddresses() {
        return new ArrayList<>(addresses);
    }
    
    // Custom setter for addresses to ensure defensive copy
    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses != null ? new ArrayList<>(addresses) : new ArrayList<>();
    }
    
    // Validation
    private void validateProfile() {
        if (firstName == null || firstName.trim().isEmpty()) {
            throw new IllegalArgumentException("First name cannot be null or empty");
        }
        if (lastName == null || lastName.trim().isEmpty()) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }
        if (phone != null && !isValidPhone(phone)) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }
    
    private boolean isValidPhone(String phone) {
        // Basic phone validation - can be enhanced based on requirements
        return phone.matches("^\\+?[1-9]\\d{1,14}$");
    }
}