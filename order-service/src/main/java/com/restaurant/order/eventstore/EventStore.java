package com.restaurant.order.eventstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.order.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Event Store service for persisting and retrieving domain events.
 * Implements Event Sourcing pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventStore {
    
    private final EventStoreRepository repository;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public void saveEvent(OrderEvent event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            
            EventStoreEntry entry = new EventStoreEntry(
                event.getEventId(),
                event.getOrderId(),
                event.getEventType(),
                eventData,
                event.getVersion(),
                event.getOccurredOn()
            );
            
            repository.save(entry);
            log.debug("Saved event: {} for order: {}", event.getEventType(), event.getOrderId());
            
        } catch (Exception e) {
            log.error("Failed to save event: {}", event.getEventType(), e);
            throw new RuntimeException("Failed to save event", e);
        }
    }
    
    @Transactional
    public void saveEvents(List<OrderEvent> events) {
        events.forEach(this::saveEvent);
    }
    
    @Transactional(readOnly = true)
    public List<OrderEvent> getEvents(String orderId) {
        List<EventStoreEntry> entries = repository.findByAggregateIdOrderByVersionAsc(orderId);
        List<OrderEvent> events = new ArrayList<>();
        
        for (EventStoreEntry entry : entries) {
            try {
                OrderEvent event = deserializeEvent(entry);
                events.add(event);
            } catch (Exception e) {
                log.error("Failed to deserialize event: {}", entry.getEventType(), e);
            }
        }
        
        return events;
    }
    
    private OrderEvent deserializeEvent(EventStoreEntry entry) throws Exception {
        return switch (entry.getEventType()) {
            case "OrderCreated" -> objectMapper.readValue(entry.getEventData(), OrderCreatedEvent.class);
            case "OrderConfirmed" -> objectMapper.readValue(entry.getEventData(), OrderConfirmedEvent.class);
            case "OrderCancelled" -> objectMapper.readValue(entry.getEventData(), OrderCancelledEvent.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + entry.getEventType());
        };
    }
}
