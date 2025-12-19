package com.restaurant.events.processing;

/**
 * Exception thrown when event processing fails.
 */
public class EventProcessingException extends RuntimeException {
    
    public EventProcessingException(String message) {
        super(message);
    }
    
    public EventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}