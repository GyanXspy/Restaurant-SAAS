package com.restaurant.events.publisher;

import com.restaurant.events.DomainEvent;

/**
 * Interface for resolving Kafka topics based on domain events.
 * Allows flexible topic routing strategies.
 */
public interface TopicResolver {
    
    /**
     * Resolves the appropriate Kafka topic for a given domain event.
     * 
     * @param event the domain event
     * @return the topic name to publish the event to
     */
    String resolveTopicForEvent(DomainEvent event);
}