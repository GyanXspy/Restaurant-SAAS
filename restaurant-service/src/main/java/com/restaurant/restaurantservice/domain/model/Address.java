package com.restaurant.restaurantservice.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * Value object representing a restaurant address.
 * Encapsulates address validation and formatting logic.
 */
@Getter
@EqualsAndHashCode
@ToString(of = {"street", "city", "zipCode", "country"})
public class Address {
    
    private final String street;
    private final String city;
    private final String zipCode;
    private final String country;

    public Address(String street, String city, String zipCode, String country) {
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
        
        this.street = street.trim();
        this.city = city.trim();
        this.zipCode = zipCode.trim();
        this.country = country.trim();
    }

    public String getFormattedAddress() {
        return String.format("%s, %s %s, %s", street, city, zipCode, country);
    }
}