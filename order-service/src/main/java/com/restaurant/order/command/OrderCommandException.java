package com.restaurant.order.command;

/**
 * Exception thrown when command handling fails.
 */
public class OrderCommandException extends RuntimeException {
    
    public OrderCommandException(String message) {
        super(message);
    }
    
    public OrderCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}