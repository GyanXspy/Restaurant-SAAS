package com.restaurant.order.command;

import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.order.event.*;
import com.restaurant.order.event.OrderCreatedEvent.OrderItemData;
import com.restaurant.order.eventstore.EventStore;
import com.restaurant.order.model.Order;
import com.restaurant.order.model.OrderStatus;
import com.restaurant.order.projection.OrderProjectionService;
import com.restaurant.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Command Handler for Order write operations with Event Sourcing.
 * Stores events in event store, publishes to Kafka, and rebuilds state from events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCommandHandler {
    
    private final OrderRepository orderRepository;
    private final OrderProjectionService projectionService;
    private final EventStore eventStore;
    private final EventPublisher eventPublisher;
    
    @Transactional
    public String handle(CreateOrderCommand command) {
        log.info("Handling CreateOrderCommand for customer: {}", command.getCustomerId());
        
        // Create order ID
        String orderId = java.util.UUID.randomUUID().toString();
        
        // Create event
        List<OrderItemData> itemsData = command.getItems().stream()
            .map(item -> new OrderItemData(
                item.getMenuItemId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity()
            ))
            .collect(Collectors.toList());
        
        OrderCreatedEvent event = new OrderCreatedEvent(
            orderId,
            command.getCustomerId(),
            command.getRestaurantId(),
            command.getTotalAmount(),
            itemsData,
            1
        );
        
        // Save event to event store
        eventStore.saveEvent(event);
        
        // Rebuild order from events
        Order order = Order.fromEvents(List.of(event));
        
        // Save current state (snapshot)
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());
        
        // Project to read model
        projectionService.projectOrder(savedOrder);
        
        // Publish event to Kafka
        eventPublisher.publish(event);
        log.info("Published OrderCreatedEvent to Kafka for order: {}", savedOrder.getId());
        
        return savedOrder.getId();
    }
    
    @Transactional
    public void handle(ConfirmOrderCommand command) {
        log.info("Handling ConfirmOrderCommand for order: {}", command.getOrderId());
        
        Order order = orderRepository.findById(command.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + command.getOrderId()));
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order cannot be confirmed in status: " + order.getStatus());
        }
        
        // Create event
        OrderConfirmedEvent event = new OrderConfirmedEvent(
            order.getId(),
            command.getPaymentId(),
            order.getVersion() + 1
        );
        
        // Save event to event store
        eventStore.saveEvent(event);
        
        // Apply event to order
        order.applyEvent(event, false);
        
        // Save current state (snapshot)
        Order savedOrder = orderRepository.save(order);
        log.info("Order confirmed: {}", command.getOrderId());
        
        // Project to read model
        projectionService.projectOrder(savedOrder);
        
        // Publish event to Kafka
        eventPublisher.publish(event);
        log.info("Published OrderConfirmedEvent to Kafka for order: {}", command.getOrderId());
    }
    
    @Transactional
    public void handle(CancelOrderCommand command) {
        log.info("Handling CancelOrderCommand for order: {}", command.getOrderId());
        
        Order order = orderRepository.findById(command.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + command.getOrderId()));
        
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel confirmed order: " + command.getOrderId());
        }
        
        // Create event
        OrderCancelledEvent event = new OrderCancelledEvent(
            order.getId(),
            command.getReason(),
            order.getVersion() + 1
        );
        
        // Save event to event store
        eventStore.saveEvent(event);
        
        // Apply event to order
        order.applyEvent(event, false);
        
        // Save current state (snapshot)
        Order savedOrder = orderRepository.save(order);
        log.info("Order cancelled: {}", command.getOrderId());
        
        // Project to read model
        projectionService.projectOrder(savedOrder);
        
        // Publish event to Kafka
        eventPublisher.publish(event);
        log.info("Published OrderCancelledEvent to Kafka for order: {}", command.getOrderId());
    }
    
    /**
     * Rebuild order from event store (useful for debugging or recovery)
     */
    @Transactional(readOnly = true)
    public Order rebuildFromEvents(String orderId) {
        List<OrderEvent> events = eventStore.getEvents(orderId);
        if (events.isEmpty()) {
            throw new IllegalArgumentException("No events found for order: " + orderId);
        }
        return Order.fromEvents(events);
    }
}
