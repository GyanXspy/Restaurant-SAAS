package com.restaurant.order.domain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Value object representing an item in an order.
 * Immutable and contains all necessary information about the ordered item.
 */
@Getter
@EqualsAndHashCode
@ToString
public class OrderItem {
    
    private final String itemId;
    private final String name;
    private final BigDecimal price;
    private final int quantity;
    
    public OrderItem(String itemId, String name, BigDecimal price, int quantity) {
        validateOrderItem(itemId, name, price, quantity);
        
        this.itemId = itemId;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
    }
    
    private void validateOrderItem(String itemId, String name, BigDecimal price, int quantity) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID is required");
        }
        
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name is required");
        }
        
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Item price must be greater than zero");
        }
        
        if (quantity <= 0) {
            throw new IllegalArgumentException("Item quantity must be greater than zero");
        }
    }
    
    /**
     * Calculates the total price for this item (price * quantity).
     */
    public BigDecimal getTotalPrice() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}