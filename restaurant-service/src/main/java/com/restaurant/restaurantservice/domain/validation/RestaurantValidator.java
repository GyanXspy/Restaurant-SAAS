package com.restaurant.restaurantservice.domain.validation;

import java.util.ArrayList;
import java.util.List;

import com.restaurant.restaurantservice.domain.model.Restaurant;

/**
 * Validator for Restaurant domain objects.
 * Provides comprehensive validation rules for restaurants.
 */
public class RestaurantValidator {
    
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_CUISINE_LENGTH = 50;
    private static final int MAX_MENU_SIZE = 200;

    /**
     * Validates a restaurant and returns list of validation errors.
     */
    public static List<String> validate(Restaurant restaurant) {
        List<String> errors = new ArrayList<>();
        
        if (restaurant == null) {
            errors.add("Restaurant cannot be null");
            return errors;
        }
        
        // Validate name
        if (restaurant.getName() == null || restaurant.getName().trim().isEmpty()) {
            errors.add("Restaurant name is required");
        } else if (restaurant.getName().length() > MAX_NAME_LENGTH) {
            errors.add("Restaurant name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }
        
        // Validate cuisine
        if (restaurant.getCuisine() == null || restaurant.getCuisine().trim().isEmpty()) {
            errors.add("Restaurant cuisine is required");
        } else if (restaurant.getCuisine().length() > MAX_CUISINE_LENGTH) {
            errors.add("Restaurant cuisine cannot exceed " + MAX_CUISINE_LENGTH + " characters");
        }
        
        // Validate address
        if (restaurant.getAddress() == null) {
            errors.add("Restaurant address is required");
        }
        
        // Validate menu size
        if (restaurant.getMenu().size() > MAX_MENU_SIZE) {
            errors.add("Restaurant menu cannot exceed " + MAX_MENU_SIZE + " items");
        }
        
        // Validate menu items
        restaurant.getMenu().forEach(menuItem -> {
            List<String> menuItemErrors = MenuItemValidator.validate(menuItem);
            menuItemErrors.forEach(error -> errors.add("Menu item validation: " + error));
        });
        
        return errors;
    }

    /**
     * Validates a restaurant and throws exception if invalid.
     */
    public static void validateAndThrow(Restaurant restaurant) {
        List<String> errors = validate(restaurant);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Restaurant validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Checks if a restaurant is valid.
     */
    public static boolean isValid(Restaurant restaurant) {
        return validate(restaurant).isEmpty();
    }
}