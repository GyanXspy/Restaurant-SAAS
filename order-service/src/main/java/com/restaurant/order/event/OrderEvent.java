package com.restaurant.order.event;

import com.restaurant.events.DomainEvent;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Base class for all Order domain events.
 * Events are immutable records of state changes.
 * Extends DomainEvent for Kafka publishing.
 */
@Getter
public abstract class OrderEvent extends DomainEvent {
    
    protected OrderEvent(String orderId, int version) {
        super(orderId, version);
    }
    
    protected OrderEvent(String eventId, String orderId, LocalDateTime occurredOn, int version) {
        super(eventId, orderId, occurredOn, version);
    }
    
    // Alias for compatibility
    public String getOrderId() {
        return getAggregateId();
    }
}
