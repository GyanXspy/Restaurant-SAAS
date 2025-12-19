package com.restaurant.events.processing;

import com.restaurant.events.DomainEvent;

/**
 * Interface for idempotent event processing.
 * Ensures that events are processed exactly once, even if received multiple times.
 */
public interface IdempotentEventProcessor {
    
    /**
     * Processes an event idempotently.
     * If the event has already been processed, this method should return without side effects.
     * 
     * @param event the domain event to process
     * @return true if the event was processed, false if it was already processed before
     */
    boolean processEvent(DomainEvent event);
    
    /**
     * Checks if an event has already been processed.
     * 
     * @param eventId the unique event identifier
     * @return true if the event has been processed before
     */
    boolean isEventProcessed(String eventId);
    
    /**
     * Marks an event as processed.
     * 
     * @param eventId the unique event identifier
     */
    void markEventAsProcessed(String eventId);
}