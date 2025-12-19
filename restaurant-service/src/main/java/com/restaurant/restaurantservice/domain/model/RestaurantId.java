package com.restaurant.restaurantservice.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a Restaurant identifier.
 * Ensures type safety and encapsulates restaurant ID logic.
 */
public class RestaurantId {
    
    private final String value;

    private RestaurantId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant ID cannot be null or empty");
        }
        this.value = value;
    }

    public static RestaurantId generate() {
        return new RestaurantId(UUID.randomUUID().toString());
    }

    public static RestaurantId of(String value) {
        return new RestaurantId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestaurantId that = (RestaurantId) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}