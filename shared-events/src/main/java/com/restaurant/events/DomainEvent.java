package com.restaurant.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events in the restaurant ordering system.
 * Provides common properties and behavior for event sourcing and event-driven communication.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEvent.class, name = "OrderCreated"),
    @JsonSubTypes.Type(value = OrderConfirmedEvent.class, name = "OrderConfirmed"),
    @JsonSubTypes.Type(value = OrderCancelledEvent.class, name = "OrderCancelled"),
    @JsonSubTypes.Type(value = OrderSagaStartedEvent.class, name = "OrderSagaStarted"),
    @JsonSubTypes.Type(value = CartValidationRequestedEvent.class, name = "CartValidationRequested"),
    @JsonSubTypes.Type(value = CartValidationCompletedEvent.class, name = "CartValidationCompleted"),
    @JsonSubTypes.Type(value = PaymentInitiationRequestedEvent.class, name = "PaymentInitiationRequested"),
    @JsonSubTypes.Type(value = PaymentProcessingCompletedEvent.class, name = "PaymentProcessingCompleted"),
    @JsonSubTypes.Type(value = UserCreatedEvent.class, name = "UserCreated"),
    @JsonSubTypes.Type(value = UserUpdatedEvent.class, name = "UserUpdated"),
    @JsonSubTypes.Type(value = UserDeactivatedEvent.class, name = "UserDeactivated"),
    @JsonSubTypes.Type(value = RestaurantCreatedEvent.class, name = "RestaurantCreated"),
    @JsonSubTypes.Type(value = MenuUpdatedEvent.class, name = "MenuUpdated")
})
public abstract class DomainEvent {
    
    private final String eventId;
    private final String aggregateId;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private final LocalDateTime occurredOn;
    
    private final int version;

    protected DomainEvent(String aggregateId, int version) {
        this.eventId = UUID.randomUUID().toString();
        this.aggregateId = aggregateId;
        this.occurredOn = LocalDateTime.now();
        this.version = version;
    }

    // Constructor for deserialization
    protected DomainEvent(String eventId, String aggregateId, LocalDateTime occurredOn, int version) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.occurredOn = occurredOn;
        this.version = version;
    }

    public String getEventId() {
        return eventId;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public int getVersion() {
        return version;
    }

    /**
     * Returns the event type name for routing and deserialization
     */
    public abstract String getEventType();

    @Override
    public String toString() {
        return String.format("%s{eventId='%s', aggregateId='%s', occurredOn=%s, version=%d}",
                getClass().getSimpleName(), eventId, aggregateId, occurredOn, version);
    }
}