package com.restaurant.order.infrastructure;

/**
 * Exception thrown when there are issues with persisting or loading Order aggregates
 * from the event store. This is a runtime exception to avoid forcing callers
 * to handle persistence-specific exceptions.
 */
public class OrderPersistenceException extends RuntimeException {
    
    public OrderPersistenceException(String message) {
        super(message);
    }
    
    public OrderPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public OrderPersistenceException(Throwable cause) {
        super(cause);
    }
}