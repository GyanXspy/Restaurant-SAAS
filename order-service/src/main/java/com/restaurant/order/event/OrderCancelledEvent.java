package com.restaurant.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCancelledEvent extends OrderEvent {
    
    private String reason;
    
    public OrderCancelledEvent(String orderId, String reason, int version) {
        super(orderId, version);
        this.reason = reason;
    }
    
    // Constructor for Jackson deserialization
    @JsonCreator
    public OrderCancelledEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("aggregateId") String orderId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("version") int version,
            @JsonProperty("reason") String reason) {
        super(eventId, orderId, occurredOn, version);
        this.reason = reason;
    }
    
    @Override
    public String getEventType() {
        return "OrderCancelled";
    }
}
