package com.restaurant.order.domain;

import com.restaurant.events.DomainEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation test to ensure Order aggregate with event sourcing is working correctly.
 * This test validates the complete implementation of task 7.2.
 */
class OrderAggregateValidationTest {

    @Test
    void orderAggregateEventSourcing_CompleteWorkflow_ShouldWorkCorrectly() {
        // Test data
        String customerId = "customer-123";
        String restaurantId = "restaurant-456";
        String paymentId = "payment-789";
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Pizza Margherita", new BigDecimal("15.99"), 1),
            new OrderItem("item-2", "Coca Cola", new BigDecimal("3.99"), 2)
        );
        BigDecimal totalAmount = new BigDecimal("23.97");

        // 1. Create Order - should generate OrderCreatedEvent
        Order order = Order.createOrder(customerId, restaurantId, items, totalAmount);
        
        // Validate initial state
        assertNotNull(order.getOrderId());
        assertEquals(customerId, order.getCustomerId());
        assertEquals(restaurantId, order.getRestaurantId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(totalAmount, order.getTotalAmount());
        assertEquals(2, order.getItems().size());
        assertEquals(1, order.getVersion());
        
        // Validate OrderCreatedEvent was generated
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();
        assertEquals(1, uncommittedEvents.size());
        assertTrue(uncommittedEvents.get(0) instanceof com.restaurant.events.OrderCreatedEvent);
        
        com.restaurant.events.OrderCreatedEvent createdEvent = 
            (com.restaurant.events.OrderCreatedEvent) uncommittedEvents.get(0);
        assertEquals(order.getOrderId(), createdEvent.getAggregateId());
        assertEquals(customerId, createdEvent.getCustomerId());
        assertEquals(restaurantId, createdEvent.getRestaurantId());
        assertEquals(totalAmount, createdEvent.getTotalAmount());
        assertEquals(2, createdEvent.getItems().size());

        // 2. Simulate event persistence (mark as committed)
        List<DomainEvent> eventsToStore = order.getUncommittedEvents();
        order.markEventsAsCommitted();
        assertTrue(order.getUncommittedEvents().isEmpty());

        // 3. Reconstruct Order from events (simulate loading from event store)
        Order reconstructedOrder = Order.fromEvents(eventsToStore);
        
        // Validate reconstructed state matches original
        assertEquals(order.getOrderId(), reconstructedOrder.getOrderId());
        assertEquals(order.getCustomerId(), reconstructedOrder.getCustomerId());
        assertEquals(order.getRestaurantId(), reconstructedOrder.getRestaurantId());
        assertEquals(order.getStatus(), reconstructedOrder.getStatus());
        assertEquals(order.getTotalAmount(), reconstructedOrder.getTotalAmount());
        assertEquals(order.getVersion(), reconstructedOrder.getVersion());
        assertEquals(order.getItems().size(), reconstructedOrder.getItems().size());

        // 4. Confirm Order - should generate OrderConfirmedEvent
        reconstructedOrder.confirmOrder(paymentId);
        
        // Validate state after confirmation
        assertEquals(OrderStatus.CONFIRMED, reconstructedOrder.getStatus());
        assertEquals(paymentId, reconstructedOrder.getPaymentId());
        assertEquals(2, reconstructedOrder.getVersion());
        
        // Validate OrderConfirmedEvent was generated
        List<DomainEvent> confirmationEvents = reconstructedOrder.getUncommittedEvents();
        assertEquals(1, confirmationEvents.size());
        assertTrue(confirmationEvents.get(0) instanceof com.restaurant.events.OrderConfirmedEvent);
        
        com.restaurant.events.OrderConfirmedEvent confirmedEvent = 
            (com.restaurant.events.OrderConfirmedEvent) confirmationEvents.get(0);
        assertEquals(reconstructedOrder.getOrderId(), confirmedEvent.getAggregateId());
        assertEquals(paymentId, confirmedEvent.getPaymentId());

        // 5. Test complete event replay with both events
        List<DomainEvent> allEvents = Arrays.asList(
            eventsToStore.get(0),  // OrderCreatedEvent
            confirmationEvents.get(0)  // OrderConfirmedEvent
        );
        
        Order fullyReconstructedOrder = Order.fromEvents(allEvents);
        
        // Validate final state
        assertEquals(OrderStatus.CONFIRMED, fullyReconstructedOrder.getStatus());
        assertEquals(paymentId, fullyReconstructedOrder.getPaymentId());
        assertEquals(2, fullyReconstructedOrder.getVersion());
        assertTrue(fullyReconstructedOrder.getUncommittedEvents().isEmpty());
    }

    @Test
    void orderAggregateEventSourcing_CancellationWorkflow_ShouldWorkCorrectly() {
        // Test data
        String customerId = "customer-123";
        String restaurantId = "restaurant-456";
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 1)
        );
        BigDecimal totalAmount = new BigDecimal("15.99");
        String cancellationReason = "Payment failed";

        // 1. Create and cancel order
        Order order = Order.createOrder(customerId, restaurantId, items, totalAmount);
        List<DomainEvent> creationEvents = order.getUncommittedEvents();
        order.markEventsAsCommitted();
        
        order.cancelOrder(cancellationReason);
        
        // Validate cancellation state
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(2, order.getVersion());
        
        // Validate OrderCancelledEvent was generated
        List<DomainEvent> cancellationEvents = order.getUncommittedEvents();
        assertEquals(1, cancellationEvents.size());
        assertTrue(cancellationEvents.get(0) instanceof com.restaurant.events.OrderCancelledEvent);
        
        com.restaurant.events.OrderCancelledEvent cancelledEvent = 
            (com.restaurant.events.OrderCancelledEvent) cancellationEvents.get(0);
        assertEquals(order.getOrderId(), cancelledEvent.getAggregateId());
        assertEquals(cancellationReason, cancelledEvent.getReason());

        // 2. Test reconstruction from all events
        List<DomainEvent> allEvents = Arrays.asList(
            creationEvents.get(0),  // OrderCreatedEvent
            cancellationEvents.get(0)  // OrderCancelledEvent
        );
        
        Order reconstructedOrder = Order.fromEvents(allEvents);
        
        // Validate final cancelled state
        assertEquals(OrderStatus.CANCELLED, reconstructedOrder.getStatus());
        assertEquals(2, reconstructedOrder.getVersion());
        assertTrue(reconstructedOrder.getUncommittedEvents().isEmpty());
    }

    @Test
    void orderBusinessRules_ValidationScenarios_ShouldEnforceCorrectly() {
        // Test invalid order creation scenarios
        List<OrderItem> validItems = Arrays.asList(
            new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 1)
        );
        BigDecimal validTotal = new BigDecimal("15.99");

        // Invalid customer ID
        assertThrows(IllegalArgumentException.class, 
            () -> Order.createOrder(null, "restaurant-1", validItems, validTotal));
        assertThrows(IllegalArgumentException.class, 
            () -> Order.createOrder("", "restaurant-1", validItems, validTotal));

        // Invalid restaurant ID
        assertThrows(IllegalArgumentException.class, 
            () -> Order.createOrder("customer-1", null, validItems, validTotal));
        assertThrows(IllegalArgumentException.class, 
            () -> Order.createOrder("customer-1", "", validItems, validTotal));

        // Invalid items
        assertThrows(IllegalArgumentException.class, 
            () -> Order.createOrder("customer-1", "restaurant-1", null, validTotal));
        assertThrows(IllegalArgumentException.class, 
            () -> Order.createOrder("customer-1", "restaurant-1", List.of(), validTotal));

        // Invalid total amount
        assertThrows(IllegalArgumentException.class, 
            () -> Order.createOrder("customer-1", "restaurant-1", validItems, null));
        assertThrows(IllegalArgumentException.class, 
            () -> Order.createOrder("customer-1", "restaurant-1", validItems, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, 
            () -> Order.createOrder("customer-1", "restaurant-1", validItems, new BigDecimal("99.99")));

        // Test state transition validation
        Order order = Order.createOrder("customer-1", "restaurant-1", validItems, validTotal);
        
        // Invalid confirmation
        assertThrows(IllegalArgumentException.class, () -> order.confirmOrder(null));
        assertThrows(IllegalArgumentException.class, () -> order.confirmOrder(""));
        
        // Valid confirmation
        order.confirmOrder("payment-123");
        
        // Cannot confirm again
        assertThrows(IllegalStateException.class, () -> order.confirmOrder("payment-456"));
        
        // Cannot cancel confirmed order
        assertThrows(IllegalStateException.class, () -> order.cancelOrder("test"));
    }
}