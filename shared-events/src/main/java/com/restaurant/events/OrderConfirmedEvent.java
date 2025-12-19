package com.restaurant.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when an order is successfully confirmed after saga completion.
 */
public class OrderConfirmedEvent extends DomainEvent {
    
    private final String customerId;
    private final String restaurantId;
    private final BigDecimal totalAmount;
    private final String paymentId;

    public OrderConfirmedEvent(String orderId, String customerId, String restaurantId, 
                             BigDecimal totalAmount, String paymentId, int version) {
        super(orderId, version);
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalAmount = totalAmount;
        this.paymentId = paymentId;
    }

    @JsonCreator
    public OrderConfirmedEvent(@JsonProperty("eventId") String eventId,
                             @JsonProperty("aggregateId") String aggregateId,
                             @JsonProperty("occurredOn") LocalDateTime occurredOn,
                             @JsonProperty("version") int version,
                             @JsonProperty("customerId") String customerId,
                             @JsonProperty("restaurantId") String restaurantId,
                             @JsonProperty("totalAmount") BigDecimal totalAmount,
                             @JsonProperty("paymentId") String paymentId) {
        super(eventId, aggregateId, occurredOn, version);
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalAmount = totalAmount;
        this.paymentId = paymentId;
    }

    @Override
    public String getEventType() {
        return "OrderConfirmed";
    }

    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getPaymentId() { return paymentId; }
}