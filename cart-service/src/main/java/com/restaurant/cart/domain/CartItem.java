package com.restaurant.cart.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Value object representing an item in the shopping cart.
 */
public class CartItem {
    
    private String itemId;
    private String name;
    private BigDecimal price;
    private int quantity;
    private String restaurantId;

    public CartItem() {
        // Default constructor for MongoDB
    }

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

    // Getters
    public String getItemId() { return itemId; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public String getRestaurantId() { return restaurantId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return quantity == cartItem.quantity &&
               Objects.equals(itemId, cartItem.itemId) &&
               Objects.equals(name, cartItem.name) &&
               Objects.equals(price, cartItem.price) &&
               Objects.equals(restaurantId, cartItem.restaurantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, name, price, quantity, restaurantId);
    }

    @Override
    public String toString() {
        return "CartItem{" +
               "itemId='" + itemId + '\'' +
               ", name='" + name + '\'' +
               ", price=" + price +
               ", quantity=" + quantity +
               ", restaurantId='" + restaurantId + '\'' +
               '}';
    }
}