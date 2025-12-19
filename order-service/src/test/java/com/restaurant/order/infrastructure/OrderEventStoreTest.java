package com.restaurant.order.infrastructure;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.store.EventStore;
import com.restaurant.order.domain.Order;
import com.restaurant.order.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderEventStore focusing on event persistence and aggregate reconstruction.
 */
@ExtendWith(MockitoExtension.class)
class OrderEventStoreTest {

    @Mock
    private EventStore eventStore;

    private OrderEventStore orderEventStore;

    private static final String ORDER_ID = "order-123";
    private static final String CUSTOMER_ID = "customer-456";
    private static final String RESTAURANT_ID = "restaurant-789";
    private static final String PAYMENT_ID = "payment-abc";
    private static final BigDecimal TOTAL_AMOUNT = new BigDecimal("25.99");

    @BeforeEach
    void setUp() {
        orderEventStore = new OrderEventStore(eventStore);
    }

    @Test
    void save_WithUncommittedEvents_ShouldPersistEventsAndMarkAsCommitted() {
        // Given
        Order order = createOrderWithUncommittedEvents();
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();
        assertFalse(uncommittedEvents.isEmpty());

        // When
        orderEventStore.save(order);

        // Then
        verify(eventStore).saveEvents(uncommittedEvents);
        assertTrue(order.getUncommittedEvents().isEmpty());
    }

    @Test
    void save_WithNoUncommittedEvents_ShouldNotCallEventStore() {
        // Given
        Order order = createOrderWithUncommittedEvents();
        order.markEventsAsCommitted(); // Clear uncommitted events

        // When
        orderEventStore.save(order);

        // Then
        verify(eventStore, never()).saveEvents(any());
    }

    @Test
    void save_WhenEventStoreFails_ShouldThrowOrderPersistenceException() {
        // Given
        Order order = createOrderWithUncommittedEvents();
        RuntimeException eventStoreException = new RuntimeException("Database connection failed");
        doThrow(eventStoreException).when(eventStore).saveEvents(any());

        // When & Then
        OrderPersistenceException exception = assertThrows(
            OrderPersistenceException.class,
            () -> orderEventStore.save(order)
        );
        
        assertTrue(exception.getMessage().contains("Failed to save order"));
        assertTrue(exception.getMessage().contains(order.getOrderId()));
        assertEquals(eventStoreException, exception.getCause());
    }

    @Test
    void findById_WithExistingOrder_ShouldReturnReconstructedOrder() {
        // Given
        List<DomainEvent> events = createOrderEvents();
        when(eventStore.getEventsForAggregate(ORDER_ID)).thenReturn(events);

        // When
        Optional<Order> result = orderEventStore.findById(ORDER_ID);

        // Then
        assertTrue(result.isPresent());
        Order order = result.get();
        assertEquals(ORDER_ID, order.getOrderId());
        assertEquals(CUSTOMER_ID, order.getCustomerId());
        assertEquals(RESTAURANT_ID, order.getRestaurantId());
        assertEquals(TOTAL_AMOUNT, order.getTotalAmount());
        verify(eventStore).getEventsForAggregate(ORDER_ID);
    }

    @Test
    void findById_WithNonExistentOrder_ShouldReturnEmpty() {
        // Given
        when(eventStore.getEventsForAggregate(ORDER_ID)).thenReturn(List.of());

        // When
        Optional<Order> result = orderEventStore.findById(ORDER_ID);

        // Then
        assertFalse(result.isPresent());
        verify(eventStore).getEventsForAggregate(ORDER_ID);
    }

    @Test
    void findById_WhenEventStoreFails_ShouldThrowOrderPersistenceException() {
        // Given
        RuntimeException eventStoreException = new RuntimeException("Database query failed");
        when(eventStore.getEventsForAggregate(ORDER_ID)).thenThrow(eventStoreException);

        // When & Then
        OrderPersistenceException exception = assertThrows(
            OrderPersistenceException.class,
            () -> orderEventStore.findById(ORDER_ID)
        );
        
        assertTrue(exception.getMessage().contains("Failed to load order"));
        assertTrue(exception.getMessage().contains(ORDER_ID));
        assertEquals(eventStoreException, exception.getCause());
    }

    @Test
    void findByIdFromVersion_WithValidVersion_ShouldReturnOrderFromVersion() {
        // Given
        int fromVersion = 2;
        List<DomainEvent> events = createOrderEvents().subList(1, 2); // Only second event
        when(eventStore.getEventsForAggregateFromVersion(ORDER_ID, fromVersion)).thenReturn(events);

        // When
        Optional<Order> result = orderEventStore.findByIdFromVersion(ORDER_ID, fromVersion);

        // Then
        assertTrue(result.isPresent());
        verify(eventStore).getEventsForAggregateFromVersion(ORDER_ID, fromVersion);
    }

    @Test
    void findByIdFromVersion_WithNoEventsFromVersion_ShouldReturnEmpty() {
        // Given
        int fromVersion = 5;
        when(eventStore.getEventsForAggregateFromVersion(ORDER_ID, fromVersion)).thenReturn(List.of());

        // When
        Optional<Order> result = orderEventStore.findByIdFromVersion(ORDER_ID, fromVersion);

        // Then
        assertFalse(result.isPresent());
        verify(eventStore).getEventsForAggregateFromVersion(ORDER_ID, fromVersion);
    }

    @Test
    void findByIdFromVersion_WhenEventStoreFails_ShouldThrowOrderPersistenceException() {
        // Given
        int fromVersion = 1;
        RuntimeException eventStoreException = new RuntimeException("Version query failed");
        when(eventStore.getEventsForAggregateFromVersion(ORDER_ID, fromVersion))
            .thenThrow(eventStoreException);

        // When & Then
        OrderPersistenceException exception = assertThrows(
            OrderPersistenceException.class,
            () -> orderEventStore.findByIdFromVersion(ORDER_ID, fromVersion)
        );
        
        assertTrue(exception.getMessage().contains("Failed to load order from version"));
        assertTrue(exception.getMessage().contains(ORDER_ID));
        assertTrue(exception.getMessage().contains("version: " + fromVersion));
        assertEquals(eventStoreException, exception.getCause());
    }

    @Test
    void getCurrentVersion_WithExistingOrder_ShouldReturnCurrentVersion() {
        // Given
        int expectedVersion = 3;
        when(eventStore.getCurrentVersion(ORDER_ID)).thenReturn(expectedVersion);

        // When
        int actualVersion = orderEventStore.getCurrentVersion(ORDER_ID);

        // Then
        assertEquals(expectedVersion, actualVersion);
        verify(eventStore).getCurrentVersion(ORDER_ID);
    }

    @Test
    void getCurrentVersion_WithNonExistentOrder_ShouldReturnZero() {
        // Given
        when(eventStore.getCurrentVersion(ORDER_ID)).thenReturn(0);

        // When
        int actualVersion = orderEventStore.getCurrentVersion(ORDER_ID);

        // Then
        assertEquals(0, actualVersion);
        verify(eventStore).getCurrentVersion(ORDER_ID);
    }

    @Test
    void getCurrentVersion_WhenEventStoreFails_ShouldThrowOrderPersistenceException() {
        // Given
        RuntimeException eventStoreException = new RuntimeException("Version query failed");
        when(eventStore.getCurrentVersion(ORDER_ID)).thenThrow(eventStoreException);

        // When & Then
        OrderPersistenceException exception = assertThrows(
            OrderPersistenceException.class,
            () -> orderEventStore.getCurrentVersion(ORDER_ID)
        );
        
        assertTrue(exception.getMessage().contains("Failed to get current version for order"));
        assertTrue(exception.getMessage().contains(ORDER_ID));
        assertEquals(eventStoreException, exception.getCause());
    }

    @Test
    void exists_WithExistingOrder_ShouldReturnTrue() {
        // Given
        when(eventStore.getCurrentVersion(ORDER_ID)).thenReturn(2);

        // When
        boolean exists = orderEventStore.exists(ORDER_ID);

        // Then
        assertTrue(exists);
        verify(eventStore).getCurrentVersion(ORDER_ID);
    }

    @Test
    void exists_WithNonExistentOrder_ShouldReturnFalse() {
        // Given
        when(eventStore.getCurrentVersion(ORDER_ID)).thenReturn(0);

        // When
        boolean exists = orderEventStore.exists(ORDER_ID);

        // Then
        assertFalse(exists);
        verify(eventStore).getCurrentVersion(ORDER_ID);
    }

    @Test
    void getOrderEvents_WithExistingOrder_ShouldReturnAllEvents() {
        // Given
        List<DomainEvent> expectedEvents = createOrderEvents();
        when(eventStore.getEventsForAggregate(ORDER_ID)).thenReturn(expectedEvents);

        // When
        List<DomainEvent> actualEvents = orderEventStore.getOrderEvents(ORDER_ID);

        // Then
        assertEquals(expectedEvents, actualEvents);
        verify(eventStore).getEventsForAggregate(ORDER_ID);
    }

    @Test
    void getOrderEvents_WhenEventStoreFails_ShouldThrowOrderPersistenceException() {
        // Given
        RuntimeException eventStoreException = new RuntimeException("Events query failed");
        when(eventStore.getEventsForAggregate(ORDER_ID)).thenThrow(eventStoreException);

        // When & Then
        OrderPersistenceException exception = assertThrows(
            OrderPersistenceException.class,
            () -> orderEventStore.getOrderEvents(ORDER_ID)
        );
        
        assertTrue(exception.getMessage().contains("Failed to get events for order"));
        assertTrue(exception.getMessage().contains(ORDER_ID));
        assertEquals(eventStoreException, exception.getCause());
    }

    private Order createOrderWithUncommittedEvents() {
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item-1", "Pizza", new BigDecimal("12.99"), 1),
            new OrderItem("item-2", "Drink", new BigDecimal("13.00"), 1)
        );
        return Order.createOrder(CUSTOMER_ID, RESTAURANT_ID, items, TOTAL_AMOUNT);
    }

    private List<DomainEvent> createOrderEvents() {
        List<com.restaurant.events.OrderCreatedEvent.OrderItem> eventItems = Arrays.asList(
            new com.restaurant.events.OrderCreatedEvent.OrderItem("item-1", "Pizza", new BigDecimal("12.99"), 1),
            new com.restaurant.events.OrderCreatedEvent.OrderItem("item-2", "Drink", new BigDecimal("13.00"), 1)
        );

        return Arrays.asList(
            new com.restaurant.events.OrderCreatedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, eventItems, TOTAL_AMOUNT, 1),
            new com.restaurant.events.OrderConfirmedEvent(ORDER_ID, CUSTOMER_ID, RESTAURANT_ID, TOTAL_AMOUNT, PAYMENT_ID, 2)
        );
    }
}