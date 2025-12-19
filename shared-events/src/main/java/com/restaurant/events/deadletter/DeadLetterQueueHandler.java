package com.restaurant.events.deadletter;

import com.restaurant.events.DomainEvent;

/**
 * Interface for handling events that failed processing and were sent to dead letter queue.
 */
public interface DeadLetterQueueHandler {
    
    /**
     * Handles a failed event from the dead letter queue.
     * 
     * @param failedEvent the original event that failed
     * @param failureReason the reason for failure
     * @param attemptCount the number of processing attempts
     */
    void handleFailedEvent(DomainEvent failedEvent, String failureReason, int attemptCount);
    
    /**
     * Attempts to reprocess a failed event.
     * 
     * @param failedEvent the event to reprocess
     * @return true if reprocessing was successful
     */
    boolean reprocessEvent(DomainEvent failedEvent);
}