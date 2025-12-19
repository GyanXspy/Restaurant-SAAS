package com.restaurant.cart.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Cart aggregate representing a customer's shopping cart.
 */
@Document(collection = "carts")
public class Cart {
    
    @Id
    private String cartId;
    private String customerId;
    private String restaurantId;
    private List<CartItem> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private CartStatus status;

    public Cart() {
        // Default constructor for MongoDB
        this.items = new ArrayList<>();
    }

    public Cart(String cartId, String customerId) {
        if (cartId == null || cartId.trim().isEmpty()) {
            throw new IllegalArgumentException("Cart ID cannot be null or empty");
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }

        this.cartId = cartId;
        this.customerId = customerId;
        this.items = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusHours(24); // Cart expires in 24 hours
        this.status = CartStatus.ACTIVE;
    }

    public void addItem(CartItem item) {
        validateCartIsActive();
        
        // Ensure all items are from the same restaurant
        if (restaurantId == null) {
            this.restaurantId = item.getRestaurantId();
        } else if (!restaurantId.equals(item.getRestaurantId())) {
            throw new IllegalArgumentException("Cannot add items from different restaurants to the same cart");
        }

        // Check if item already exists, if so, update quantity
        Optional<CartItem> existingItem = items.stream()
            .filter(cartItem -> cartItem.getItemId().equals(item.getItemId()))
            .findFirst();

        if (existingItem.isPresent()) {
            items.remove(existingItem.get());
            CartItem updatedItem = existingItem.get().withQuantity(
                existingItem.get().getQuantity() + item.getQuantity()
            );
            items.add(updatedItem);
        } else {
            items.add(item);
        }

        this.updatedAt = LocalDateTime.now();
    }

    public void removeItem(String itemId) {
        validateCartIsActive();
        
        items.removeIf(item -> item.getItemId().equals(itemId));
        this.updatedAt = LocalDateTime.now();
        
        // If no items left, clear restaurant ID
        if (items.isEmpty()) {
            this.restaurantId = null;
        }
    }

    public void updateItemQuantity(String itemId, int newQuantity) {
        validateCartIsActive();
        
        if (newQuantity <= 0) {
            removeItem(itemId);
            return;
        }

        Optional<CartItem> existingItem = items.stream()
            .filter(item -> item.getItemId().equals(itemId))
            .findFirst();

        if (existingItem.isPresent()) {
            items.remove(existingItem.get());
            items.add(existingItem.get().withQuantity(newQuantity));
            this.updatedAt = LocalDateTime.now();
        } else {
            throw new IllegalArgumentException("Item not found in cart: " + itemId);
        }
    }

    public void clearCart() {
        validateCartIsActive();
        
        this.items.clear();
        this.restaurantId = null;
        this.updatedAt = LocalDateTime.now();
    }

    public BigDecimal getTotalAmount() {
        return items.stream()
            .map(CartItem::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getTotalItemCount() {
        return items.stream()
            .mapToInt(CartItem::getQuantity)
            .sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void markAsExpired() {
        this.status = CartStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsCheckedOut() {
        this.status = CartStatus.CHECKED_OUT;
        this.updatedAt = LocalDateTime.now();
    }

    public List<String> validateForCheckout() {
        List<String> validationErrors = new ArrayList<>();

        if (isEmpty()) {
            validationErrors.add("Cart is empty");
        }

        if (isExpired()) {
            validationErrors.add("Cart has expired");
        }

        if (status != CartStatus.ACTIVE) {
            validationErrors.add("Cart is not active");
        }

        if (restaurantId == null) {
            validationErrors.add("No restaurant selected");
        }

        // Additional business rule validations can be added here
        // For example: minimum order amount, item availability, etc.

        return validationErrors;
    }

    private void validateCartIsActive() {
        if (status != CartStatus.ACTIVE) {
            throw new IllegalStateException("Cannot modify cart in status: " + status);
        }
        if (isExpired()) {
            throw new IllegalStateException("Cannot modify expired cart");
        }
    }

    // Getters
    public String getCartId() { return cartId; }
    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public List<CartItem> getItems() { return new ArrayList<>(items); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public CartStatus getStatus() { return status; }

    @Override
    public String toString() {
        return "Cart{" +
               "cartId='" + cartId + '\'' +
               ", customerId='" + customerId + '\'' +
               ", restaurantId='" + restaurantId + '\'' +
               ", itemCount=" + items.size() +
               ", totalAmount=" + getTotalAmount() +
               ", status=" + status +
               ", createdAt=" + createdAt +
               ", updatedAt=" + updatedAt +
               ", expiresAt=" + expiresAt +
               '}';
    }
}