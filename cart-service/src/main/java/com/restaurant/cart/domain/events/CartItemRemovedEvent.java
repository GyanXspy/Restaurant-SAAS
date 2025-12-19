package com.restaurant.cart.domain.events;

import java.math.BigDecimal;

import com.restaurant.events.DomainEvent;

/**
 * Domain event published when an item is removed from a cart.
 */
public class CartItemRemovedEvent extends DomainEvent {
    
    private final String cartId;
    private final String customerId;
    private final String itemId;
    private final BigDecimal totalAmount;

    public CartItemRemovedEvent(String cartId, String customerId, String itemId, 
                               BigDecimal totalAmount, int version) {
        super(cartId, version);
        this.cartId = cartId;
        this.customerId = customerId;
        this.itemId = itemId;
        this.totalAmount = totalAmount;
    }

    @Override
    public String getEventType() {
        return "CartItemRemoved";
    }

    // Getters
    public String getCartId() { return cartId; }
    public String getCustomerId() { return customerId; }
    public String getItemId() { return itemId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}