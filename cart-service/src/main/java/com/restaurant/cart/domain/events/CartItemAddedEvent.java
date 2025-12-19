package com.restaurant.cart.domain.events;

import java.math.BigDecimal;

import com.restaurant.events.DomainEvent;

/**
 * Domain event published when an item is added to a cart.
 */
public class CartItemAddedEvent extends DomainEvent {
    
    private final String cartId;
    private final String customerId;
    private final String restaurantId;
    private final String itemId;
    private final String itemName;
    private final BigDecimal itemPrice;
    private final int quantity;
    private final BigDecimal totalAmount;

    public CartItemAddedEvent(String cartId, String customerId, String restaurantId, 
                             String itemId, String itemName, BigDecimal itemPrice, 
                             int quantity, BigDecimal totalAmount, int version) {
        super(cartId, version);
        this.cartId = cartId;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemPrice = itemPrice;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    @Override
    public String getEventType() {
        return "CartItemAdded";
    }

    // Getters
    public String getCartId() { return cartId; }
    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public String getItemId() { return itemId; }
    public String getItemName() { return itemName; }
    public BigDecimal getItemPrice() { return itemPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}