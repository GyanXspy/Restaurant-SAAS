package com.restaurant.cart.domain.events;

import com.restaurant.events.DomainEvent;

/**
 * Domain event published when a cart is cleared.
 */
public class CartClearedEvent extends DomainEvent {
    
    private final String cartId;
    private final String customerId;

    public CartClearedEvent(String cartId, String customerId, int version) {
        super(cartId, version);
        this.cartId = cartId;
        this.customerId = customerId;
    }

    @Override
    public String getEventType() {
        return "CartCleared";
    }

    // Getters
    public String getCartId() { return cartId; }
    public String getCustomerId() { return customerId; }
}