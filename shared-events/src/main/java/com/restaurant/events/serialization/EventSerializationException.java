package com.restaurant.events.serialization;

/**
 * Exception thrown when event serialization or deserialization fails.
 */
public class EventSerializationException extends RuntimeException {
    
    public EventSerializationException(String message) {
        super(message);
    }
    
    public EventSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}