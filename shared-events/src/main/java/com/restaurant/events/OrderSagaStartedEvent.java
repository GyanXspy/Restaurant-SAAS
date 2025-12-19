package com.restaurant.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when an order saga orchestration process is started.
 * This event initiates the distributed transaction flow for order processing.
 */
public class OrderSagaStartedEvent extends DomainEvent {
    
    private final String customerId;
    private final String restaurantId;
    private final List<OrderCreatedEvent.OrderItem> items;
    private final BigDecimal totalAmount;

    public OrderSagaStartedEvent(String orderId, String customerId, String restaurantId, 
                               List<OrderCreatedEvent.OrderItem> items, BigDecimal totalAmount, int version) {
        super(orderId, version);
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    @JsonCreator
    public OrderSagaStartedEvent(@JsonProperty("eventId") String eventId,
                               @JsonProperty("aggregateId") String aggregateId,
                               @JsonProperty("occurredOn") LocalDateTime occurredOn,
                               @JsonProperty("version") int version,
                               @JsonProperty("customerId") String customerId,
                               @JsonProperty("restaurantId") String restaurantId,
                               @JsonProperty("items") List<OrderCreatedEvent.OrderItem> items,
                               @JsonProperty("totalAmount") BigDecimal totalAmount) {
        super(eventId, aggregateId, occurredOn, version);
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    @Override
    public String getEventType() {
        return "OrderSagaStarted";
    }

    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public List<OrderCreatedEvent.OrderItem> getItems() { return items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}