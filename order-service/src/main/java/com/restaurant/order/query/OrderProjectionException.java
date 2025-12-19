package com.restaurant.order.query;

/**
 * Exception thrown when event projection fails.
 */
public class OrderProjectionException extends RuntimeException {
    
    public OrderProjectionException(String message) {
        super(message);
    }
    
    public OrderProjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}