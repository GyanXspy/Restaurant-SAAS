package com.restaurant.user.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Address {
    
    private AddressType type;
    private String street;
    private String city;
    private String state;
    private String zipCode;
    private String country;
    
    // Constructor with validation
    public Address(AddressType type, String street, String city, String state, String zipCode, String country) {
        this.type = type;
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        validateAddress();
    }
    
    // Validation
    private void validateAddress() {
        if (type == null) {
            throw new IllegalArgumentException("Address type cannot be null");
        }
        if (street == null || street.trim().isEmpty()) {
            throw new IllegalArgumentException("Street cannot be null or empty");
        }
        if (city == null || city.trim().isEmpty()) {
            throw new IllegalArgumentException("City cannot be null or empty");
        }
        if (zipCode == null || zipCode.trim().isEmpty()) {
            throw new IllegalArgumentException("Zip code cannot be null or empty");
        }
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("Country cannot be null or empty");
        }
    }
    
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(street);
        if (city != null) {
            sb.append(", ").append(city);
        }
        if (state != null) {
            sb.append(", ").append(state);
        }
        if (zipCode != null) {
            sb.append(" ").append(zipCode);
        }
        if (country != null) {
            sb.append(", ").append(country);
        }
        return sb.toString();
    }
}