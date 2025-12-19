package com.restaurant.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Event published when an order is cancelled due to saga failure or compensation.
 */
public class OrderCancelledEvent extends DomainEvent {
    
    private final String customerId;
    private final String reason;

    public OrderCancelledEvent(String orderId, String customerId, String reason, int version) {
        super(orderId, version);
        this.customerId = customerId;
        this.reason = reason;
    }

    @JsonCreator
    public OrderCancelledEvent(@JsonProperty("eventId") String eventId,
                             @JsonProperty("aggregateId") String aggregateId,
                             @JsonProperty("occurredOn") LocalDateTime occurredOn,
                             @JsonProperty("version") int version,
                             @JsonProperty("customerId") String customerId,
                             @JsonProperty("reason") String reason) {
        super(eventId, aggregateId, occurredOn, version);
        this.customerId = customerId;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "OrderCancelled";
    }

    public String getCustomerId() { return customerId; }
    public String getReason() { return reason; }
}