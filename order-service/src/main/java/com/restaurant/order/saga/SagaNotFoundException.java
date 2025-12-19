package com.restaurant.order.saga;

/**
 * Exception thrown when a saga is not found in the repository.
 */
public class SagaNotFoundException extends RuntimeException {
    
    public SagaNotFoundException(String message) {
        super(message);
    }
    
    public SagaNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}