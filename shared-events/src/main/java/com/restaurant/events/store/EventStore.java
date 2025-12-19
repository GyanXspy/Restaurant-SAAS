package com.restaurant.events.store;

import java.util.List;

import com.restaurant.events.DomainEvent;

/**
 * Interface for storing and retrieving domain events.
 * Supports event sourcing pattern by providing methods to persist events
 * and replay them to reconstruct aggregate state.
 */
public interface EventStore {
    
    /**
     * Saves a single domain event to the event store.
     * 
     * @param event the domain event to save
     * @throws EventStoreException if the event cannot be saved
     */
    void saveEvent(DomainEvent event);
    
    /**
     * Saves multiple domain events atomically.
     * All events must be saved successfully or none at all.
     * 
     * @param events the list of domain events to save
     * @throws EventStoreException if any event cannot be saved
     */
    void saveEvents(List<DomainEvent> events);
    
    /**
     * Retrieves all events for a specific aggregate in chronological order.
     * 
     * @param aggregateId the unique identifier of the aggregate
     * @return list of events ordered by occurrence time
     */
    List<DomainEvent> getEventsForAggregate(String aggregateId);
    
    /**
     * Retrieves events for a specific aggregate starting from a given version.
     * Useful for incremental event replay.
     * 
     * @param aggregateId the unique identifier of the aggregate
     * @param fromVersion the version to start from (inclusive)
     * @return list of events from the specified version onwards
     */
    List<DomainEvent> getEventsForAggregateFromVersion(String aggregateId, int fromVersion);
    
    /**
     * Retrieves all events of a specific type.
     * Useful for building read model projections.
     * 
     * @param eventType the class type of the events to retrieve
     * @param <T> the event type
     * @return list of events of the specified type
     */
    <T extends DomainEvent> List<T> getEventsByType(Class<T> eventType);
    
    /**
     * Gets the current version of an aggregate based on stored events.
     * 
     * @param aggregateId the unique identifier of the aggregate
     * @return the current version, or 0 if no events exist for the aggregate
     */
    int getCurrentVersion(String aggregateId);
}