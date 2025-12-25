package com.restaurant.order.model;

import com.restaurant.order.event.*;
import com.restaurant.order.event.OrderCreatedEvent.OrderItemData;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
public class Order {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(nullable = false)
    private String customerId;
    
    @Column(nullable = false)
    private String restaurantId;
    
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private List<OrderItem> items = new ArrayList<>();
    
    @Column(nullable = false)
    private BigDecimal totalAmount;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    
    private String paymentId;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    @Version
    private int version;
    
    // Transient field for uncommitted events
    @Transient
    private List<OrderEvent> uncommittedEvents = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = OrderStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Event Sourcing: Rebuild from events
    public static Order fromEvents(List<OrderEvent> events) {
        Order order = new Order();
        for (OrderEvent event : events) {
            order.applyEvent(event, false);
        }
        return order;
    }
    
    // Apply event and optionally add to uncommitted events
    public void applyEvent(OrderEvent event, boolean isNew) {
        if (event instanceof OrderCreatedEvent) {
            apply((OrderCreatedEvent) event);
        } else if (event instanceof OrderConfirmedEvent) {
            apply((OrderConfirmedEvent) event);
        } else if (event instanceof OrderCancelledEvent) {
            apply((OrderCancelledEvent) event);
        } else {
            throw new IllegalArgumentException("Unknown event type");
        }
        
        if (isNew) {
            uncommittedEvents.add(event);
        }
    }
    
    private void apply(OrderCreatedEvent event) {
        this.id = event.getOrderId();
        this.customerId = event.getCustomerId();
        this.restaurantId = event.getRestaurantId();
        this.totalAmount = event.getTotalAmount();
        this.status = OrderStatus.PENDING;
        this.createdAt = event.getOccurredOn();
        this.updatedAt = event.getOccurredOn();
        this.version = event.getVersion();
        
        this.items = event.getItems().stream()
            .map(item -> new OrderItem(item.getMenuItemId(), item.getName(), 
                                      item.getPrice(), item.getQuantity()))
            .collect(Collectors.toList());
    }
    
    private void apply(OrderConfirmedEvent event) {
        this.status = OrderStatus.CONFIRMED;
        this.paymentId = event.getPaymentId();
        this.updatedAt = event.getOccurredOn();
        this.version = event.getVersion();
    }
    
    private void apply(OrderCancelledEvent event) {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = event.getOccurredOn();
        this.version = event.getVersion();
    }
    
    public List<OrderEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }
    
    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }
}
