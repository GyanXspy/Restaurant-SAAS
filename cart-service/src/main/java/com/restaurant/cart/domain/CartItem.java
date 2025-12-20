package com.restaurant.cart.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Value object representing an item in the shopping cart.
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class CartItem {
    
    private String itemId;
    private String name;
    private BigDecimal price;
    private int quantity;
    private String restaurantId;

    public CartItem(String itemId, String name, BigDecimal price, int quantity, String restaurantId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Item price must be positive");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Item quantity must be positive");
        }
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant ID cannot be null or empty");
        }

        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.restaurantId = restaurantId;
    }

    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    public CartItem withQuantity(int newQuantity) {
        return new CartItem(itemId, name, price, newQuantity, restaurantId);
    }
}