package com.restaurant.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderConfirmedEvent extends OrderEvent {
    
    private String paymentId;
    
    public OrderConfirmedEvent(String orderId, String paymentId, int version) {
        super(orderId, version);
        this.paymentId = paymentId;
    }
    
    // Constructor for Jackson deserialization
    @JsonCreator
    public OrderConfirmedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("aggregateId") String orderId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("version") int version,
            @JsonProperty("paymentId") String paymentId) {
        super(eventId, orderId, occurredOn, version);
        this.paymentId = paymentId;
    }
    
    @Override
    public String getEventType() {
        return "OrderConfirmed";
    }
}
