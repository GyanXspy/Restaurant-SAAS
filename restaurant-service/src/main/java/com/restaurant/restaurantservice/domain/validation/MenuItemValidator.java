package com.restaurant.restaurantservice.domain.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.restaurant.restaurantservice.domain.model.MenuItem;

/**
 * Validator for MenuItem domain objects.
 * Provides comprehensive validation rules for menu items.
 */
public class MenuItemValidator {
    
    private static final BigDecimal MAX_PRICE = new BigDecimal("999.99");
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_CATEGORY_LENGTH = 50;

    /**
     * Validates a menu item and returns list of validation errors.
     */
    public static List<String> validate(MenuItem menuItem) {
        List<String> errors = new ArrayList<>();
        
        if (menuItem == null) {
            errors.add("Menu item cannot be null");
            return errors;
        }
        
        // Validate name
        if (menuItem.getName() == null || menuItem.getName().trim().isEmpty()) {
            errors.add("Menu item name is required");
        } else if (menuItem.getName().length() > MAX_NAME_LENGTH) {
            errors.add("Menu item name cannot exceed " + MAX_NAME_LENGTH + " characters");
        }
        
        // Validate price
        if (menuItem.getPrice() == null) {
            errors.add("Menu item price is required");
        } else if (menuItem.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Menu item price cannot be negative");
        } else if (menuItem.getPrice().compareTo(MAX_PRICE) > 0) {
            errors.add("Menu item price cannot exceed " + MAX_PRICE);
        }
        
        // Validate category
        if (menuItem.getCategory() == null || menuItem.getCategory().trim().isEmpty()) {
            errors.add("Menu item category is required");
        } else if (menuItem.getCategory().length() > MAX_CATEGORY_LENGTH) {
            errors.add("Menu item category cannot exceed " + MAX_CATEGORY_LENGTH + " characters");
        }
        
        // Validate description
        if (menuItem.getDescription() != null && menuItem.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            errors.add("Menu item description cannot exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        
        return errors;
    }

    /**
     * Validates a menu item and throws exception if invalid.
     */
    public static void validateAndThrow(MenuItem menuItem) {
        List<String> errors = validate(menuItem);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Menu item validation failed: " + String.join(", ", errors));
        }
    }

    /**
     * Checks if a menu item is valid.
     */
    public static boolean isValid(MenuItem menuItem) {
        return validate(menuItem).isEmpty();
    }
}