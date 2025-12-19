package com.restaurant.order.domain;

import com.restaurant.events.DomainEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Order aggregate focusing on event sourcing behavior,
 * business logic validation, and state transitions.
 */
class OrderTest {

    private static final String CUSTOMER_ID = "customer-123";
    private static final String RESTAURANT_ID = "restaurant-456";
    private static final String PAYMENT_ID = "payment-789";
    private static final BigDecimal ITEM_PRICE = new BigDecimal("15.99");
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("31.98");

    @Test
    void createOrder_WithValidData_ShouldCreateOrderWithPendingStatus() {
        // Given
        List<OrderItem> items = createValidOrderItems();

        // When
        Order order = Order.createOrder(CUSTOMER_ID, RESTAURANT_ID, items, TOTAL_AMOUNT);

        // Then
        assertNotNull(order.getOrderId());
        assertEquals(CUSTOMER_ID, order.getCustomerId());
        assertEquals(RESTAURANT_ID, order.getRestaurantId());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        assertEquals(TOTAL_AMOUNT, order.getTotalAmount());
        assertEquals(2, order.getItems().size());
        assertEquals(1, order.getVersion());
        assertNotNull(order.getCreatedAt());
        assertNotNull(order.getUpdatedAt());

        // Verify uncommitted events
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();
        assertEquals(1, uncommittedEvents.size());
        assertTrue(uncommittedEvents.get(0) instanceof com.restaurant.events.OrderCreatedEvent);
    }

    @Test
    void createOrder_WithNullCustomerId_ShouldThrowException() {
        // Given
        List<OrderItem> items = createValidOrderItems();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createOrder(null, RESTAURANT_ID, items, TOTAL_AMOUNT)
        );
        assertEquals("Customer ID is required", exception.getMessage());
    }

    @Test
    void createOrder_WithEmptyCustomerId_ShouldThrowException() {
        // Given
        List<OrderItem> items = createValidOrderItems();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createOrder("", RESTAURANT_ID, items, TOTAL_AMOUNT)
        );
        assertEquals("Customer ID is required", exception.getMessage());
    }

    @Test
    void createOrder_WithNullRestaurantId_ShouldThrowException() {
        // Given
        List<OrderItem> items = createValidOrderItems();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createOrder(CUSTOMER_ID, null, items, TOTAL_AMOUNT)
        );
        assertEquals("Restaurant ID is required", exception.getMessage());
    }

    @Test
    void createOrder_WithEmptyItems_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createOrder(CUSTOMER_ID, RESTAURANT_ID, List.of(), TOTAL_AMOUNT)
        );
        assertEquals("Order must contain at least one item", exception.getMessage());
    }

    @Test
    void createOrder_WithNullItems_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createOrder(CUSTOMER_ID, RESTAURANT_ID, null, TOTAL_AMOUNT)
        );
        assertEquals("Order must contain at least one item", exception.getMessage());
    }

    @Test
    void createOrder_WithZeroTotalAmount_ShouldThrowException() {
        // Given
        List<OrderItem> items = createValidOrderItems();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createOrder(CUSTOMER_ID, RESTAURANT_ID, items, BigDecimal.ZERO)
        );
        assertEquals("Total amount must be greater than zero", exception.getMessage());
    }

    @Test
    void createOrder_WithIncorrectTotalAmount_ShouldThrowException() {
        // Given
        List<OrderItem> items = createValidOrderItems();
        BigDecimal incorrectTotal = new BigDecimal("50.00");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Order.createOrder(CUSTOMER_ID, RESTAURANT_ID, items, incorrectTotal)
        );
        assertEquals("Provided total amount does not match calculated total", exception.getMessage());
    }

    @Test
    void confirmOrder_WithValidPaymentId_ShouldConfirmOrder() {
        // Given
        Order order = createValidOrder();

        // When
        order.confirmOrder(PAYMENT_ID);

        // Then
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(PAYMENT_ID, order.getPaymentId());
        assertEquals(2, order.getVersion());

        // Verify uncommitted events
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();
        assertEquals(2, uncommittedEvents.size());
        assertTrue(uncommittedEvents.get(1) instanceof com.restaurant.events.OrderConfirmedEvent);
    }

    @Test
    void confirmOrder_WithNullPaymentId_ShouldThrowException() {
        // Given
        Order order = createValidOrder();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> order.confirmOrder(null)
        );
        assertEquals("Payment ID is required to confirm order", exception.getMessage());
    }

    @Test
    void confirmOrder_WithEmptyPaymentId_ShouldThrowException() {
        // Given
        Order order = createValidOrder();

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> order.confirmOrder("")
        );
        assertEquals("Payment ID is required to confirm order", exception.getMessage());
    }

    @Test
    void confirmOrder_WhenAlreadyConfirmed_ShouldThrowException() {
        // Given
        Order order = createValidOrder();
        order.confirmOrder(PAYMENT_ID);
        order.markEventsAsCommitted();

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> order.confirmOrder("another-payment-id")
        );
        assertEquals("Order can only be confirmed when in PENDING status", exception.getMessage());
    }

    @Test
    void confirmOrder_WhenCancelled_ShouldThrowException() {
        // Given
        Order order = createValidOrder();
        order.cancelOrder("Test cancellation");
        order.markEventsAsCommitted();

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> order.confirmOrder(PAYMENT_ID)
        );
        assertEquals("Order can only be confirmed when in PENDING status", exception.getMessage());
    }

    @Test
    void cancelOrder_WithValidReason_ShouldCancelOrder() {
        // Given
        Order order = createValidOrder();
        String reason = "Payment failed";

        // When
        order.cancelOrder(reason);

        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(2, order.getVersion());

        // Verify uncommitted events
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();
        assertEquals(2, uncommittedEvents.size());
        assertTrue(uncommittedEvents.get(1) instanceof com.restaurant.events.OrderCancelledEvent);
        
        com.restaurant.events.OrderCancelledEvent cancelEvent = (com.restaurant.events.OrderCancelledEvent) uncommittedEvents.get(1);
        assertEquals(reason, cancelEvent.getReason());
    }

    @Test
    void cancelOrder_WhenAlreadyCancelled_ShouldBeNoOp() {
        // Given
        Order order = createValidOrder();
        order.cancelOrder("First cancellation");
        int versionAfterFirstCancel = order.getVersion();
        order.markEventsAsCommitted();

        // When
        order.cancelOrder("Second cancellation");

        // Then
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(versionAfterFirstCancel, order.getVersion());
        assertTrue(order.getUncommittedEvents().isEmpty());
    }

    @Test
    void cancelOrder_WhenConfirmed_ShouldThrowException() {
        // Given
        Order order = createValidOrder();
        order.confirmOrder(PAYMENT_ID);
        order.markEventsAsCommitted();

        // When & Then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> order.cancelOrder("Cannot cancel confirmed order")
        );
        assertEquals("Cannot cancel a confirmed order", exception.getMessage());
    }

    @Test
    void fromEvents_WithValidEventSequence_ShouldReconstructOrder() {
        // Given
        String orderId = "order-123";
        List<DomainEvent> events = Arrays.asList(
            new com.restaurant.events.OrderCreatedEvent(orderId, CUSTOMER_ID, RESTAURANT_ID, 
                                createEventItems(), TOTAL_AMOUNT, 1),
            new com.restaurant.events.OrderConfirmedEvent(orderId, CUSTOMER_ID, RESTAURANT_ID, 
                                  TOTAL_AMOUNT, PAYMENT_ID, 2)
        );

        // When
        Order order = Order.fromEvents(events);

        // Then
        assertEquals(orderId, order.getOrderId());
        assertEquals(CUSTOMER_ID, order.getCustomerId());
        assertEquals(RESTAURANT_ID, order.getRestaurantId());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(PAYMENT_ID, order.getPaymentId());
        assertEquals(TOTAL_AMOUNT, order.getTotalAmount());
        assertEquals(2, order.getVersion());
        assertEquals(2, order.getItems().size());
        assertTrue(order.getUncommittedEvents().isEmpty());
    }

    @Test
    void fromEvents_WithCancelledOrder_ShouldReconstructCancelledOrder() {
        // Given
        String orderId = "order-123";
        List<DomainEvent> events = Arrays.asList(
            new com.restaurant.events.OrderCreatedEvent(orderId, CUSTOMER_ID, RESTAURANT_ID, 
                                createEventItems(), TOTAL_AMOUNT, 1),
            new com.restaurant.events.OrderCancelledEvent(orderId, CUSTOMER_ID, "Payment failed", 2)
        );

        // When
        Order order = Order.fromEvents(events);

        // Then
        assertEquals(orderId, order.getOrderId());
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(2, order.getVersion());
        assertTrue(order.getUncommittedEvents().isEmpty());
    }

    @Test
    void fromEvents_WithEmptyEventList_ShouldThrowException() {
        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> Order.fromEvents(List.of())
        );
        assertEquals("Cannot reconstruct Order from empty event list", exception.getMessage());
    }

    @Test
    void markEventsAsCommitted_ShouldClearUncommittedEvents() {
        // Given
        Order order = createValidOrder();
        assertFalse(order.getUncommittedEvents().isEmpty());

        // When
        order.markEventsAsCommitted();

        // Then
        assertTrue(order.getUncommittedEvents().isEmpty());
    }

    @Test
    void getUncommittedEvents_ShouldReturnCopyAndClearOriginal() {
        // Given
        Order order = createValidOrder();
        order.confirmOrder(PAYMENT_ID);

        // When
        List<DomainEvent> events = order.getUncommittedEvents();

        // Then
        assertEquals(2, events.size());
        assertTrue(order.getUncommittedEvents().isEmpty()); // Should be cleared after getting
    }

    private Order createValidOrder() {
        List<OrderItem> items = createValidOrderItems();
        return Order.createOrder(CUSTOMER_ID, RESTAURANT_ID, items, TOTAL_AMOUNT);
    }

    private List<OrderItem> createValidOrderItems() {
        return Arrays.asList(
            new OrderItem("item-1", "Pizza Margherita", ITEM_PRICE, 1),
            new OrderItem("item-2", "Coca Cola", ITEM_PRICE, 1)
        );
    }

    private List<com.restaurant.events.OrderCreatedEvent.OrderItem> createEventItems() {
        return Arrays.asList(
            new com.restaurant.events.OrderCreatedEvent.OrderItem("item-1", "Pizza Margherita", ITEM_PRICE, 1),
            new com.restaurant.events.OrderCreatedEvent.OrderItem("item-2", "Coca Cola", ITEM_PRICE, 1)
        );
    }
}