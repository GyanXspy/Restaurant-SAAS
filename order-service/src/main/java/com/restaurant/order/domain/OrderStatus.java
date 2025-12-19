package com.restaurant.order.domain;

/**
 * Enumeration representing the possible states of an order in the system.
 * Used to track order progression through the saga orchestration process.
 */
public enum OrderStatus {
    
    /**
     * Order has been created and is waiting for processing.
     * This is the initial state when an order is first created.
     */
    PENDING,
    
    /**
     * Order has been successfully processed and confirmed.
     * Payment has been completed and the order is ready for fulfillment.
     */
    CONFIRMED,
    
    /**
     * Order has been cancelled due to validation failure, payment failure,
     * or other business reasons during the saga process.
     */
    CANCELLED;
    
    /**
     * Checks if the order is in a final state (cannot be changed further).
     */
    public boolean isFinalState() {
        return this == CONFIRMED || this == CANCELLED;
    }
    
    /**
     * Checks if the order can be cancelled from its current state.
     */
    public boolean canBeCancelled() {
        return this == PENDING;
    }
    
    /**
     * Checks if the order can be confirmed from its current state.
     */
    public boolean canBeConfirmed() {
        return this == PENDING;
    }
}