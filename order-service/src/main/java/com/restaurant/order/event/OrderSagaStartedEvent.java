package com.restaurant.order.event;

import com.restaurant.events.DomainEvent;
import com.restaurant.order.domain.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class OrderSagaStartedEvent extends DomainEvent {
    
    private final String customerId;
    private final String restaurantId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;

    public OrderSagaStartedEvent(String orderId, String customerId, String restaurantId, 
                               List<OrderItem> items, BigDecimal totalAmount, int version) {
        super(orderId, version);
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    public OrderSagaStartedEvent(String eventId, String aggregateId, LocalDateTime occurredOn, int version,
                               String customerId, String restaurantId, List<OrderItem> items, BigDecimal totalAmount) {
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

    public String getCustomerId() {
        return customerId;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }
}