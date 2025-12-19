package com.restaurant.order.infrastructure.resilience;

import com.restaurant.events.publisher.EventPublisher;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Resilient wrapper for EventPublisher that implements circuit breaker,
 * bulkhead, and retry patterns for Kafka event publishing.
 */
@Component
@Qualifier("resilientEventPublisher")
public class ResilientEventPublisher implements EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientEventPublisher.class);
    
    private final EventPublisher delegate;
    
    @Autowired
    public ResilientEventPublisher(@Qualifier("kafkaEventPublisher") EventPublisher delegate) {
        this.delegate = delegate;
    }
    
    @Override
    @CircuitBreaker(name = "event-publisher", fallbackMethod = "fallbackPublish")
    @Retry(name = "event-publisher")
    @Bulkhead(name = "event-publisher", fallbackMethod = "fallbackPublish")
    public void publish(String topic, Object event) {
        logger.debug("Publishing event to topic: {} through resilient publisher", topic);
        
        try {
            delegate.publish(topic, event);
            logger.debug("Successfully published event to topic: {}", topic);
        } catch (Exception e) {
            logger.error("Failed to publish event to topic: {}", topic, e);
            throw e;
        }
    }
    
    @Override
    @CircuitBreaker(name = "event-publisher", fallbackMethod = "fallbackPublishWithKey")
    @Retry(name = "event-publisher")
    @Bulkhead(name = "event-publisher", fallbackMethod = "fallbackPublishWithKey")
    public void publish(String topic, String key, Object event) {
        logger.debug("Publishing event with key: {} to topic: {} through resilient publisher", key, topic);
        
        try {
            delegate.publish(topic, key, event);
            logger.debug("Successfully published event with key: {} to topic: {}", key, topic);
        } catch (Exception e) {
            logger.error("Failed to publish event with key: {} to topic: {}", key, topic, e);
            throw e;
        }
    }
    
    /**
     * Fallback method for event publishing without key
     */
    public void fallbackPublish(String topic, Object event, Exception ex) {
        logger.warn("Event publishing fallback triggered for topic: {}, reason: {}", 
                   topic, ex.getMessage());
        
        // Store event for later retry or send to dead letter queue
        storeFailedEvent(topic, null, event, ex);
        
        // Throw exception to indicate failure to the caller
        throw new EventPublishingException(
            "Event publishing temporarily unavailable for topic: " + topic, ex);
    }
    
    /**
     * Fallback method for event publishing with key
     */
    public void fallbackPublishWithKey(String topic, String key, Object event, Exception ex) {
        logger.warn("Event publishing fallback triggered for topic: {}, key: {}, reason: {}", 
                   topic, key, ex.getMessage());
        
        // Store event for later retry or send to dead letter queue
        storeFailedEvent(topic, key, event, ex);
        
        // Throw exception to indicate failure to the caller
        throw new EventPublishingException(
            "Event publishing temporarily unavailable for topic: " + topic + ", key: " + key, ex);
    }
    
    /**
     * Store failed events for later processing or dead letter queue
     */
    private void storeFailedEvent(String topic, String key, Object event, Exception ex) {
        try {
            // In a real implementation, this could:
            // 1. Store in a local database table for retry
            // 2. Send to a dead letter queue
            // 3. Write to a file for manual processing
            // 4. Send to a monitoring system
            
            logger.info("Storing failed event for later processing - Topic: {}, Key: {}, Event: {}", 
                       topic, key, event.getClass().getSimpleName());
            
            // For now, just log the failure
            // In production, implement proper dead letter queue handling
            
        } catch (Exception storeEx) {
            logger.error("Failed to store failed event for topic: {}, key: {}", topic, key, storeEx);
        }
    }
    
    /**
     * Custom exception for event publishing failures
     */
    public static class EventPublishingException extends RuntimeException {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}