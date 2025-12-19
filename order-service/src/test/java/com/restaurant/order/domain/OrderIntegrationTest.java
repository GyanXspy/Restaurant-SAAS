package com.restaurant.order.domain;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.OrderCreatedEvent;
import com.restaurant.events.OrderConfirmedEvent;
import com.restaurant.events.OrderCancelledEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Integration test to validate Order aggregate event sourcing functionality.
 * This test verifies the complete event sourcing workflow.
 */
public class OrderIntegrationTest {

    @Test
    public void testCompleteOrderLifecycle() {
        // Test data
        String customerId = "customer-123";
        String restaurantId = "restaurant-456";
        String paymentId = "payment-789";
        BigDecimal totalAmount = new BigDecimal("25.99");
        
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 1),
            new OrderItem("item-2", "Drink", new BigDecimal("10.00"), 1)
        );

        // 1. Create order
        Order order = Order.createOrder(customerId, restaurantId, items, totalAmount);
        
        // Verify initial state
        assert order.getOrderId() != null;
        assert order.getCustomerId().equals(customerId);
        assert order.getRestaurantId().equals(restaurantId);
        assert order.getStatus() == OrderStatus.PENDING;
        assert order.getTotalAmount().equals(totalAmount);
        assert order.getVersion() == 1;
        
        // Verify uncommitted events
        List<DomainEvent> events = order.getUncommittedEvents();
        assert events.size() == 1;
        assert events.get(0) instanceof OrderCreatedEvent;
        
        // 2. Confirm order
        order.confirmOrder(paymentId);
        
        // Verify confirmed state
        assert order.getStatus() == OrderStatus.CONFIRMED;
        assert order.getPaymentId().equals(paymentId);
        assert order.getVersion() == 2;
        
        // Verify events
        events = order.getUncommittedEvents();
        assert events.size() == 2;
        assert events.get(1) instanceof OrderConfirmedEvent;
        
        // 3. Test event sourcing reconstruction
        List<DomainEvent> allEvents = order.getUncommittedEvents();
        Order reconstructedOrder = Order.fromEvents(allEvents);
        
        // Verify reconstructed order matches original
        assert reconstructedOrder.getOrderId().equals(order.getOrderId());
        assert reconstructedOrder.getCustomerId().equals(order.getCustomerId());
        assert reconstructedOrder.getRestaurantId().equals(order.getRestaurantId());
        assert reconstructedOrder.getStatus() == order.getStatus();
        assert reconstructedOrder.getPaymentId().equals(order.getPaymentId());
        assert reconstructedOrder.getTotalAmount().equals(order.getTotalAmount());
        assert reconstructedOrder.getVersion() == order.getVersion();
        
        System.out.println("✓ Order aggregate event sourcing test passed successfully!");
    }

    @Test
    public void testOrderCancellationLifecycle() {
        // Test data
        String customerId = "customer-123";
        String restaurantId = "restaurant-456";
        BigDecimal totalAmount = new BigDecimal("25.99");
        
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Pizza", new BigDecimal("25.99"), 1)
        );

        // 1. Create order
        Order order = Order.createOrder(customerId, restaurantId, items, totalAmount);
        
        // 2. Cancel order
        String cancellationReason = "Payment failed";
        order.cancelOrder(cancellationReason);
        
        // Verify cancelled state
        assert order.getStatus() == OrderStatus.CANCELLED;
        assert order.getVersion() == 2;
        
        // Verify events
        List<DomainEvent> events = order.getUncommittedEvents();
        assert events.size() == 2;
        assert events.get(1) instanceof OrderCancelledEvent;
        
        OrderCancelledEvent cancelEvent = (OrderCancelledEvent) events.get(1);
        assert cancelEvent.getReason().equals(cancellationReason);
        
        // 3. Test event sourcing reconstruction
        Order reconstructedOrder = Order.fromEvents(events);
        
        // Verify reconstructed order
        assert reconstructedOrder.getStatus() == OrderStatus.CANCELLED;
        assert reconstructedOrder.getVersion() == 2;
        
        System.out.println("✓ Order cancellation event sourcing test passed successfully!");
    }
}