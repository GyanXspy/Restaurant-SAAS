package com.restaurant.order.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.events.OrderCancelledEvent;
import com.restaurant.events.OrderConfirmedEvent;
import com.restaurant.events.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Event projection handler that updates read models based on domain events.
 * Listens to domain events and maintains the read-side projections for CQRS.
 */
@Service
@Transactional
public class OrderProjectionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderProjectionHandler.class);
    
    private final OrderReadModelRepository repository;
    private final ObjectMapper objectMapper;
    
    public OrderProjectionHandler(OrderReadModelRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handles OrderCreatedEvent to create read model projection.
     */
    @KafkaListener(topics = "order-created", groupId = "order-service-projections")
    public void handle(OrderCreatedEvent event) {
        logger.info("Projecting OrderCreatedEvent for order: {}", event.getAggregateId());
        
        try {
            // Serialize order items to JSON
            String itemsJson = serializeItems(event.getItems());
            
            // Create read model
            OrderReadModel readModel = new OrderReadModel(
                event.getAggregateId(),
                event.getCustomerId(),
                event.getRestaurantId(),
                null, // Restaurant name will be enriched later
                itemsJson,
                "PENDING",
                event.getTotalAmount(),
                null
            );
            
            readModel.setCreatedAt(event.getOccurredOn());
            readModel.setUpdatedAt(event.getOccurredOn());
            
            repository.save(readModel);
            
            logger.info("Successfully projected OrderCreatedEvent for order: {}", event.getAggregateId());
            
        } catch (Exception e) {
            logger.error("Failed to project OrderCreatedEvent for order: {}", event.getAggregateId(), e);
            throw new OrderProjectionException("Failed to project OrderCreatedEvent", e);
        }
    }
    
    /**
     * Handles OrderConfirmedEvent to update read model projection.
     */
    @KafkaListener(topics = "order-confirmed", groupId = "order-service-projections")
    public void handle(OrderConfirmedEvent event) {
        logger.info("Projecting OrderConfirmedEvent for order: {}", event.getAggregateId());
        
        try {
            OrderReadModel readModel = repository.findById(event.getAggregateId())
                .orElseThrow(() -> new OrderProjectionException(
                    "Order read model not found for confirmation: " + event.getAggregateId()));
            
            readModel.setStatus("CONFIRMED");
            readModel.setPaymentId(event.getPaymentId());
            readModel.setUpdatedAt(event.getOccurredOn());
            
            repository.save(readModel);
            
            logger.info("Successfully projected OrderConfirmedEvent for order: {}", event.getAggregateId());
            
        } catch (Exception e) {
            logger.error("Failed to project OrderConfirmedEvent for order: {}", event.getAggregateId(), e);
            throw new OrderProjectionException("Failed to project OrderConfirmedEvent", e);
        }
    }
    
    /**
     * Handles OrderCancelledEvent to update read model projection.
     */
    @KafkaListener(topics = "order-cancelled", groupId = "order-service-projections")
    public void handle(OrderCancelledEvent event) {
        logger.info("Projecting OrderCancelledEvent for order: {}", event.getAggregateId());
        
        try {
            OrderReadModel readModel = repository.findById(event.getAggregateId())
                .orElseThrow(() -> new OrderProjectionException(
                    "Order read model not found for cancellation: " + event.getAggregateId()));
            
            readModel.setStatus("CANCELLED");
            readModel.setUpdatedAt(event.getOccurredOn());
            
            repository.save(readModel);
            
            logger.info("Successfully projected OrderCancelledEvent for order: {}", event.getAggregateId());
            
        } catch (Exception e) {
            logger.error("Failed to project OrderCancelledEvent for order: {}", event.getAggregateId(), e);
            throw new OrderProjectionException("Failed to project OrderCancelledEvent", e);
        }
    }
    
    /**
     * Serializes order items to JSON string.
     */
    private String serializeItems(List<OrderCreatedEvent.OrderItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize order items: {}", items, e);
            throw new OrderProjectionException("Failed to serialize order items", e);
        }
    }
}