package com.restaurant.order.domain;

import com.restaurant.events.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Order aggregate root that implements event sourcing pattern.
 * All state changes are captured as domain events and the aggregate
 * can be reconstructed by replaying these events.
 */
public class Order {
    
    private String orderId;
    private String customerId;
    private String restaurantId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private String paymentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;
    
    // List to track uncommitted events
    private final List<DomainEvent> uncommittedEvents = new ArrayList<>();
    
    // Private constructor for event sourcing reconstruction
    private Order() {
        this.items = new ArrayList<>();
        this.version = 0;
    }
    
    /**
     * Creates a new order with the given details.
     * This is the only way to create a new Order aggregate.
     */
    public static Order createOrder(String customerId, String restaurantId, 
                                  List<OrderItem> items, BigDecimal totalAmount) {
        
        OrderBusinessRules.validateOrderCreation(customerId, restaurantId, items, totalAmount);
        
        Order order = new Order();
        String orderId = UUID.randomUUID().toString();
        
        // Apply the OrderCreatedEvent
        com.restaurant.events.OrderCreatedEvent event = new com.restaurant.events.OrderCreatedEvent(
            orderId, customerId, restaurantId, 
            mapToEventItems(items), totalAmount, 1
        );
        
        order.apply(event);
        order.markEventAsUncommitted(event);
        
        return order;
    }
    
    /**
     * Reconstructs an Order aggregate from a list of domain events.
     * Used for event sourcing to rebuild aggregate state.
     */
    public static Order fromEvents(List<DomainEvent> events) {
        if (events.isEmpty()) {
            throw new IllegalArgumentException("Cannot reconstruct Order from empty event list");
        }
        
        Order order = new Order();
        for (DomainEvent event : events) {
            order.apply(event);
        }
        
        return order;
    }
    
    /**
     * Confirms the order after successful payment processing.
     */
    public void confirmOrder(String paymentId) {
        OrderBusinessRules.validateOrderConfirmation(this, paymentId);
        
        com.restaurant.events.OrderConfirmedEvent event = new com.restaurant.events.OrderConfirmedEvent(
            this.orderId, this.customerId, this.restaurantId, 
            this.totalAmount, paymentId, this.version + 1
        );
        
        apply(event);
        markEventAsUncommitted(event);
    }
    
    /**
     * Cancels the order with the given reason.
     */
    public void cancelOrder(String reason) {
        OrderBusinessRules.validateOrderCancellation(this);
        
        if (this.status == OrderStatus.CANCELLED) {
            return; // Already cancelled, no-op
        }
        
        com.restaurant.events.OrderCancelledEvent event = new com.restaurant.events.OrderCancelledEvent(
            this.orderId, this.customerId, reason, this.version + 1
        );
        
        apply(event);
        markEventAsUncommitted(event);
    }
    
    /**
     * Applies a domain event to update the aggregate state.
     * This method handles all event types that can affect the Order aggregate.
     */
    private void apply(DomainEvent event) {
        if (event instanceof com.restaurant.events.OrderCreatedEvent) {
            applyOrderCreated((com.restaurant.events.OrderCreatedEvent) event);
        } else if (event instanceof com.restaurant.events.OrderConfirmedEvent) {
            applyOrderConfirmed((com.restaurant.events.OrderConfirmedEvent) event);
        } else if (event instanceof com.restaurant.events.OrderCancelledEvent) {
            applyOrderCancelled((com.restaurant.events.OrderCancelledEvent) event);
        } else {
            throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
        }
        
        this.version = event.getVersion();
        this.updatedAt = event.getOccurredOn();
    }
    
    private void applyOrderCreated(com.restaurant.events.OrderCreatedEvent event) {
        this.orderId = event.getAggregateId();
        this.customerId = event.getCustomerId();
        this.restaurantId = event.getRestaurantId();
        this.items = mapFromEventItems(event.getItems());
        this.totalAmount = event.getTotalAmount();
        this.status = OrderStatus.PENDING;
        this.createdAt = event.getOccurredOn();
        this.updatedAt = event.getOccurredOn();
    }
    
    private void applyOrderConfirmed(com.restaurant.events.OrderConfirmedEvent event) {
        this.status = OrderStatus.CONFIRMED;
        this.paymentId = event.getPaymentId();
    }
    
    private void applyOrderCancelled(com.restaurant.events.OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
    }
    
    private void markEventAsUncommitted(DomainEvent event) {
        this.uncommittedEvents.add(event);
    }
    
    /**
     * Returns all uncommitted events and clears the list.
     * Should be called after events are successfully persisted.
     */
    public List<DomainEvent> getUncommittedEvents() {
        List<DomainEvent> events = new ArrayList<>(this.uncommittedEvents);
        this.uncommittedEvents.clear();
        return events;
    }
    
    /**
     * Marks all uncommitted events as committed.
     * Should be called after events are successfully persisted.
     */
    public void markEventsAsCommitted() {
        this.uncommittedEvents.clear();
    }
    

    
    // Mapping methods between domain and event objects
    private static List<com.restaurant.events.OrderCreatedEvent.OrderItem> mapToEventItems(List<OrderItem> items) {
        return items.stream()
            .map(item -> new com.restaurant.events.OrderCreatedEvent.OrderItem(
                item.getItemId(), item.getName(), item.getPrice(), item.getQuantity()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    private static List<OrderItem> mapFromEventItems(List<com.restaurant.events.OrderCreatedEvent.OrderItem> eventItems) {
        return eventItems.stream()
            .map(item -> new OrderItem(
                item.getItemId(), item.getName(), item.getPrice(), item.getQuantity()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    // Getters
    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public String getRestaurantId() { return restaurantId; }
    public List<OrderItem> getItems() { return new ArrayList<>(items); }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public OrderStatus getStatus() { return status; }
    public String getPaymentId() { return paymentId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public int getVersion() { return version; }
    
    @Override
    public String toString() {
        return String.format("Order{orderId='%s', customerId='%s', restaurantId='%s', " +
                           "status=%s, totalAmount=%s, version=%d}",
                           orderId, customerId, restaurantId, status, totalAmount, version);
    }
}