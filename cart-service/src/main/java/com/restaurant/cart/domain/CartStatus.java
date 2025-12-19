package com.restaurant.cart.domain;

/**
 * Enumeration representing the status of a shopping cart.
 */
public enum CartStatus {
    ACTIVE,      // Cart is active and can be modified
    EXPIRED,     // Cart has expired and cannot be modified
    CHECKED_OUT  // Cart has been checked out and converted to an order
}