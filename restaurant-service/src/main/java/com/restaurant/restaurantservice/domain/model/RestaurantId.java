package com.restaurant.restaurantservice.domain.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

/**
 * Value object representing a Restaurant identifier.
 * Ensures type safety and encapsulates restaurant ID logic.
 */
@Getter
@EqualsAndHashCode(of = "value")
@ToString(of = "value")
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
}