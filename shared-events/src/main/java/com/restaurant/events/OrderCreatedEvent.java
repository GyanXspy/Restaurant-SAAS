package com.restaurant.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a new order is created in the system.
 * Triggers the saga orchestration process.
 */
public class OrderCreatedEvent extends DomainEvent {
    
    private final String customerId;
    private final String restaurantId;
    private final List<OrderItem> items;
    private final BigDecimal totalAmount;

    public OrderCreatedEvent(String orderId, String customerId, String restaurantId, 
                           List<OrderItem> items, BigDecimal totalAmount, int version) {
        super(orderId, version);
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    @JsonCreator
    public OrderCreatedEvent(@JsonProperty("eventId") String eventId,
                           @JsonProperty("aggregateId") String aggregateId,
                           @JsonProperty("occurredOn") LocalDateTime occurredOn,
                           @JsonProperty("version") int version,
                           @JsonProperty("customerId") String customerId,
                           @JsonProperty("restaurantId") String restaurantId,
                           @JsonProperty("items") List<OrderItem> items,
                           @JsonProperty("totalAmount") BigDecimal totalAmount) {
        super(eventId, aggregateId, occurredOn, version);
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.items = items;
        this.totalAmount = totalAmount;
    }

    @Override
    public String getEventType() {
        return "OrderCreated";
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

    public static class OrderItem {
        private final String itemId;
        private final String name;
        private final BigDecimal price;
        private final int quantity;

        @JsonCreator
        public OrderItem(@JsonProperty("itemId") String itemId,
                        @JsonProperty("name") String name,
                        @JsonProperty("price") BigDecimal price,
                        @JsonProperty("quantity") int quantity) {
            this.itemId = itemId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }

        public String getItemId() { return itemId; }
        public String getName() { return name; }
        public BigDecimal getPrice() { return price; }
        public int getQuantity() { return quantity; }
    }
}