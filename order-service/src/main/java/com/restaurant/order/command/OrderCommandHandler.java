package com.restaurant.order.command;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.order.domain.Order;
import com.restaurant.order.domain.OrderItem;
import com.restaurant.order.infrastructure.OrderEventStore;
import com.restaurant.order.saga.OrderSagaOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Command handler for Order aggregate operations.
 * Handles all write operations (commands) for the Order domain.
 * Follows CQRS pattern by separating command handling from query operations.
 */
@Service
@Transactional
public class OrderCommandHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderCommandHandler.class);
    
    private final OrderEventStore orderEventStore;
    private final EventPublisher eventPublisher;
    private final OrderSagaOrchestrator sagaOrchestrator;
    
    public OrderCommandHandler(OrderEventStore orderEventStore, EventPublisher eventPublisher, 
                              OrderSagaOrchestrator sagaOrchestrator) {
        this.orderEventStore = orderEventStore;
        this.eventPublisher = eventPublisher;
        this.sagaOrchestrator = sagaOrchestrator;
    }
    
    /**
     * Handles the creation of a new order.
     * Creates the Order aggregate, persists events, and publishes domain events.
     * 
     * @param command the create order command
     * @return the ID of the created order
     */
    public String handle(CreateOrderCommand command) {
        logger.info("Handling CreateOrderCommand for customer: {}, restaurant: {}", 
                   command.getCustomerId(), command.getRestaurantId());
        
        try {
            // Create the Order aggregate
            Order order = Order.createOrder(
                command.getCustomerId(),
                command.getRestaurantId(),
                command.getItems(),
                command.getTotalAmount()
            );
            
            // Save the order (persists events)
            orderEventStore.save(order);
            
            // Publish domain events
            List<DomainEvent> events = order.getUncommittedEvents();
            for (DomainEvent event : events) {
                eventPublisher.publish(event);
            }
            
            // Start the saga orchestration process
            sagaOrchestrator.startOrderSaga(
                order.getOrderId(),
                command.getCustomerId(),
                command.getRestaurantId(),
                command.getItems(),
                command.getTotalAmount()
            );
            
            logger.info("Successfully created order and started saga: {}", order.getOrderId());
            return order.getOrderId();
            
        } catch (Exception e) {
            logger.error("Failed to handle CreateOrderCommand for customer: {}", 
                        command.getCustomerId(), e);
            throw new OrderCommandException("Failed to create order", e);
        }
    }
    
    /**
     * Handles order confirmation after successful payment.
     * 
     * @param command the confirm order command
     */
    public void handle(ConfirmOrderCommand command) {
        logger.info("Handling ConfirmOrderCommand for order: {}", command.getOrderId());
        
        try {
            // Load the order aggregate
            Order order = orderEventStore.findById(command.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + command.getOrderId()));
            
            // Confirm the order
            order.confirmOrder(command.getPaymentId());
            
            // Save the updated order
            orderEventStore.save(order);
            
            // Publish domain events
            List<DomainEvent> events = order.getUncommittedEvents();
            for (DomainEvent event : events) {
                eventPublisher.publish(event);
            }
            
            logger.info("Successfully confirmed order: {}", command.getOrderId());
            
        } catch (OrderNotFoundException e) {
            logger.error("Order not found for confirmation: {}", command.getOrderId());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to handle ConfirmOrderCommand for order: {}", 
                        command.getOrderId(), e);
            throw new OrderCommandException("Failed to confirm order", e);
        }
    }
    
    /**
     * Handles order cancellation.
     * 
     * @param command the cancel order command
     */
    public void handle(CancelOrderCommand command) {
        logger.info("Handling CancelOrderCommand for order: {}", command.getOrderId());
        
        try {
            // Load the order aggregate
            Order order = orderEventStore.findById(command.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + command.getOrderId()));
            
            // Cancel the order
            order.cancelOrder(command.getReason());
            
            // Save the updated order
            orderEventStore.save(order);
            
            // Publish domain events
            List<DomainEvent> events = order.getUncommittedEvents();
            for (DomainEvent event : events) {
                eventPublisher.publish(event);
            }
            
            logger.info("Successfully cancelled order: {} with reason: {}", 
                       command.getOrderId(), command.getReason());
            
        } catch (OrderNotFoundException e) {
            logger.error("Order not found for cancellation: {}", command.getOrderId());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to handle CancelOrderCommand for order: {}", 
                        command.getOrderId(), e);
            throw new OrderCommandException("Failed to cancel order", e);
        }
    }
}