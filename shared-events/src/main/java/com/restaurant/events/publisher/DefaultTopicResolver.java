package com.restaurant.events.publisher;

import com.restaurant.events.DomainEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of TopicResolver that maps event types to Kafka topics.
 * Uses a configurable mapping strategy with fallback to default topic naming.
 */
public class DefaultTopicResolver implements TopicResolver {
    
    private final Map<String, String> eventTypeToTopicMap;
    private final String defaultTopicPrefix;
    
    public DefaultTopicResolver() {
        this("restaurant-events", createDefaultEventTopicMapping());
    }
    
    public DefaultTopicResolver(String defaultTopicPrefix, Map<String, String> eventTypeToTopicMap) {
        this.defaultTopicPrefix = defaultTopicPrefix;
        this.eventTypeToTopicMap = new HashMap<>(eventTypeToTopicMap);
    }
    
    @Override
    public String resolveTopicForEvent(DomainEvent event) {
        String eventType = event.getEventType();
        
        // Check if there's a specific mapping for this event type
        String topic = eventTypeToTopicMap.get(eventType);
        if (topic != null) {
            return topic;
        }
        
        // Fallback to default naming convention
        return defaultTopicPrefix + "-" + eventType.toLowerCase().replace("_", "-");
    }
    
    /**
     * Adds or updates a mapping between event type and topic.
     * 
     * @param eventType the event type name
     * @param topic the target topic name
     */
    public void addEventTypeMapping(String eventType, String topic) {
        eventTypeToTopicMap.put(eventType, topic);
    }
    
    /**
     * Creates the default mapping between event types and Kafka topics
     * based on the saga orchestration design.
     */
    private static Map<String, String> createDefaultEventTopicMapping() {
        Map<String, String> mapping = new HashMap<>();
        
        // Saga orchestration topics
        mapping.put("OrderCreated", "order-saga-started");
        mapping.put("CartValidationRequested", "cart-validation-requested");
        mapping.put("CartValidationCompleted", "cart-validation-completed");
        mapping.put("PaymentInitiationRequested", "payment-initiation-requested");
        mapping.put("PaymentProcessingCompleted", "payment-processing-completed");
        mapping.put("OrderConfirmed", "order-confirmed");
        mapping.put("OrderCancelled", "order-cancelled");
        
        // User service events
        mapping.put("UserCreated", "user-events");
        mapping.put("UserUpdated", "user-events");
        
        // Restaurant service events
        mapping.put("RestaurantCreated", "restaurant-events");
        mapping.put("MenuUpdated", "restaurant-events");
        
        return mapping;
    }
}