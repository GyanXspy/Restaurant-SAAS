package com.restaurant.events.processing;

import com.restaurant.events.DomainEvent;

/**
 * Interface for processing domain events.
 * Implementations contain the business logic for handling specific events.
 */
public interface EventProcessor {
    
    /**
     * Processes a domain event.
     * 
     * @param event the domain event to process
     * @throws EventProcessingException if processing fails
     */
    void process(DomainEvent event);
}