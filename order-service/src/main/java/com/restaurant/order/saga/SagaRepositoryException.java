package com.restaurant.order.saga;

/**
 * Exception thrown when saga repository operations fail.
 */
public class SagaRepositoryException extends RuntimeException {
    
    public SagaRepositoryException(String message) {
        super(message);
    }
    
    public SagaRepositoryException(String message, Throwable cause) {
        super(message, cause);
    }
}