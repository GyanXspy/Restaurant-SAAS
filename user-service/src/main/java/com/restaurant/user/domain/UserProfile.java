package com.restaurant.user.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserProfile {
    
    private String firstName;
    private String lastName;
    private String phone;
    private List<Address> addresses;
    
    // Default constructor
    public UserProfile() {
        this.addresses = new ArrayList<>();
    }
    
    // Constructor
    public UserProfile(String firstName, String lastName, String phone) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
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
    
    // Getters and setters
    public String getFirstName() {
        return firstName;
    }
    
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    
    public String getLastName() {
        return lastName;
    }
    
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public List<Address> getAddresses() {
        return new ArrayList<>(addresses);
    }
    
    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses != null ? new ArrayList<>(addresses) : new ArrayList<>();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return Objects.equals(firstName, that.firstName) &&
                Objects.equals(lastName, that.lastName) &&
                Objects.equals(phone, that.phone) &&
                Objects.equals(addresses, that.addresses);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(firstName, lastName, phone, addresses);
    }
    
    @Override
    public String toString() {
        return "UserProfile{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", phone='" + phone + '\'' +
                ", addresses=" + addresses +
                '}';
    }
}