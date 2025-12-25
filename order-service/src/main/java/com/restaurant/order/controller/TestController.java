package com.restaurant.order.controller;

import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.order.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Test controller for DLQ testing.
 * Provides endpoints to test event publishing without database transactions.
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    
    private final EventPublisher eventPublisher;
    
    /**
     * Test event publishing directly (no transaction).
     * Use this to test DLQ when Kafka is down.
     */
    @PostMapping("/publish-event")
    public Map<String, Object> testPublishEvent() {
        log.info("=== TEST: Publishing event directly ===");
        
        Map<String, Object> response = new HashMap<>();
        String eventId = java.util.UUID.randomUUID().toString();
        String orderId = "test-order-" + System.currentTimeMillis();
        
        try {
            // Create a test event
            OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                "test-customer-123",
                "test-restaurant-456",
                new BigDecimal("99.99"),
                new ArrayList<>(),
                1
            );
            
            log.info("Publishing test event: {}", event.getEventId());
            
            // This will fail if Kafka is down and go to DLQ
            eventPublisher.publish(event);
            
            log.info("Event published successfully");
            response.put("success", true);
            response.put("message", "Event published successfully");
            response.put("eventId", event.getEventId());
            response.put("orderId", orderId);
            
        } catch (Exception e) {
            log.error("Failed to publish event", e);
            response.put("success", false);
            response.put("message", "Failed to publish: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
        }
        
        return response;
    }
    
    /**
     * Check Kafka connectivity.
     */
    @GetMapping("/kafka-status")
    public Map<String, Object> checkKafkaStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Try to publish a simple test event
            OrderCreatedEvent event = new OrderCreatedEvent(
                "test-order-" + System.currentTimeMillis(),
                "test-customer",
                "test-restaurant",
                BigDecimal.ZERO,
                new ArrayList<>(),
                1
            );
            
            eventPublisher.publish(event);
            
            response.put("kafka", "UP");
            response.put("message", "Kafka is reachable");
            
        } catch (Exception e) {
            response.put("kafka", "DOWN");
            response.put("message", "Kafka is not reachable: " + e.getMessage());
        }
        
        return response;
    }
}
