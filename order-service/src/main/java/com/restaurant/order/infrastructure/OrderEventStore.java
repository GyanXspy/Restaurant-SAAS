package com.restaurant.order.infrastructure;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.store.EventStore;
import com.restaurant.order.domain.Order;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository implementation for Order aggregate using event sourcing.
 * Provides methods to save and load Order aggregates by persisting and replaying domain events.
 */
@Repository
public class OrderEventStore {
    
    private final EventStore eventStore;
    
    public OrderEventStore(EventStore eventStore) {
        this.eventStore = eventStore;
    }
    
    /**
     * Saves an Order aggregate by persisting its uncommitted events.
     * 
     * @param order the Order aggregate to save
     * @throws IllegalStateException if the order has no uncommitted events
     */
    public void save(Order order) {
        List<DomainEvent> uncommittedEvents = order.getUncommittedEvents();
        
        if (uncommittedEvents.isEmpty()) {
            return; // No changes to persist
        }
        
        try {
            eventStore.saveEvents(uncommittedEvents);
            order.markEventsAsCommitted();
        } catch (Exception e) {
            throw new OrderPersistenceException(
                "Failed to save order: " + order.getOrderId(), e);
        }
    }
    
    /**
     * Loads an Order aggregate by replaying all its domain events.
     * 
     * @param orderId the unique identifier of the order
     * @return Optional containing the Order if found, empty otherwise
     */
    public Optional<Order> findById(String orderId) {
        try {
            List<DomainEvent> events = eventStore.getEventsForAggregate(orderId);
            
            if (events.isEmpty()) {
                return Optional.empty();
            }
            
            Order order = Order.fromEvents(events);
            return Optional.of(order);
            
        } catch (Exception e) {
            throw new OrderPersistenceException(
                "Failed to load order: " + orderId, e);
        }
    }
    
    /**
     * Loads an Order aggregate from a specific version by replaying events from that version.
     * Useful for optimistic concurrency control and incremental updates.
     * 
     * @param orderId the unique identifier of the order
     * @param fromVersion the version to start loading from (inclusive)
     * @return Optional containing the Order if found, empty otherwise
     */
    public Optional<Order> findByIdFromVersion(String orderId, int fromVersion) {
        try {
            List<DomainEvent> events = eventStore.getEventsForAggregateFromVersion(orderId, fromVersion);
            
            if (events.isEmpty()) {
                return Optional.empty();
            }
            
            Order order = Order.fromEvents(events);
            return Optional.of(order);
            
        } catch (Exception e) {
            throw new OrderPersistenceException(
                "Failed to load order from version: " + orderId + ", version: " + fromVersion, e);
        }
    }
    
    /**
     * Gets the current version of an order without loading the full aggregate.
     * Useful for optimistic concurrency control.
     * 
     * @param orderId the unique identifier of the order
     * @return the current version of the order, or 0 if the order doesn't exist
     */
    public int getCurrentVersion(String orderId) {
        try {
            return eventStore.getCurrentVersion(orderId);
        } catch (Exception e) {
            throw new OrderPersistenceException(
                "Failed to get current version for order: " + orderId, e);
        }
    }
    
    /**
     * Checks if an order exists in the event store.
     * 
     * @param orderId the unique identifier of the order
     * @return true if the order exists, false otherwise
     */
    public boolean exists(String orderId) {
        return getCurrentVersion(orderId) > 0;
    }
    
    /**
     * Gets all events for a specific order.
     * Useful for debugging and audit purposes.
     * 
     * @param orderId the unique identifier of the order
     * @return list of all events for the order
     */
    public List<DomainEvent> getOrderEvents(String orderId) {
        try {
            return eventStore.getEventsForAggregate(orderId);
        } catch (Exception e) {
            throw new OrderPersistenceException(
                "Failed to get events for order: " + orderId, e);
        }
    }
}