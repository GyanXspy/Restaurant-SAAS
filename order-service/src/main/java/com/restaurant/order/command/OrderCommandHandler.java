package com.restaurant.order.command;

import com.restaurant.order.model.Order;
import com.restaurant.order.model.OrderItem;
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
 * Command Handler for Order write operations.
 * Handles all state-changing operations following CQRS pattern.
 * Projects changes to read model after successful write.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCommandHandler {
    
    private final OrderRepository orderRepository;
    private final OrderProjectionService projectionService;
    
    @Transactional
    public String handle(CreateOrderCommand command) {
        log.info("Handling CreateOrderCommand for customer: {}", command.getCustomerId());
        
        Order order = new Order();
        order.setCustomerId(command.getCustomerId());
        order.setRestaurantId(command.getRestaurantId());
        order.setTotalAmount(command.getTotalAmount());
        order.setStatus(OrderStatus.PENDING);
        
        List<OrderItem> items = command.getItems().stream()
            .map(itemCmd -> new OrderItem(
                itemCmd.getMenuItemId(),
                itemCmd.getName(),
                itemCmd.getPrice(),
                itemCmd.getQuantity()
            ))
            .collect(Collectors.toList());
        order.setItems(items);
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());
        
        // Project to read model
        projectionService.projectOrder(savedOrder);
        
        // TODO: Publish OrderCreatedEvent to Kafka
        
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
        
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentId(command.getPaymentId());
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order confirmed: {}", command.getOrderId());
        
        // Project to read model
        projectionService.projectOrder(savedOrder);
        
        // TODO: Publish OrderConfirmedEvent to Kafka
    }
    
    @Transactional
    public void handle(CancelOrderCommand command) {
        log.info("Handling CancelOrderCommand for order: {}", command.getOrderId());
        
        Order order = orderRepository.findById(command.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + command.getOrderId()));
        
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel confirmed order: " + command.getOrderId());
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order cancelled: {}", command.getOrderId());
        
        // Project to read model
        projectionService.projectOrder(savedOrder);
        
        // TODO: Publish OrderCancelledEvent to Kafka
    }
}
