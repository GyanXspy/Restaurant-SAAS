package com.restaurant.restaurantservice.domain.event;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.restaurant.events.DomainEvent;

/**
 * Event published when a restaurant's availability status changes.
 */
public class RestaurantAvailabilityChangedEvent extends DomainEvent {
    
    private final String restaurantId;
    private final boolean isActive;
    private final String reason;

    public RestaurantAvailabilityChangedEvent(String restaurantId, boolean isActive, String reason, int version) {
        super(restaurantId, version);
        this.restaurantId = restaurantId;
        this.isActive = isActive;
        this.reason = reason;
    }

    @JsonCreator
    public RestaurantAvailabilityChangedEvent(@JsonProperty("eventId") String eventId,
                                            @JsonProperty("aggregateId") String aggregateId,
                                            @JsonProperty("occurredOn") LocalDateTime occurredOn,
                                            @JsonProperty("version") int version,
                                            @JsonProperty("restaurantId") String restaurantId,
                                            @JsonProperty("isActive") boolean isActive,
                                            @JsonProperty("reason") String reason) {
        super(eventId, aggregateId, occurredOn, version);
        this.restaurantId = restaurantId;
        this.isActive = isActive;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "RestaurantAvailabilityChanged";
    }

    public String getRestaurantId() { return restaurantId; }
    public boolean isActive() { return isActive; }
    public String getReason() { return reason; }
}