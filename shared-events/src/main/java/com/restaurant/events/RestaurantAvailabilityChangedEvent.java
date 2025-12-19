package com.restaurant.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Event published when a restaurant's availability status changes.
 * This includes activation, deactivation, or other availability-related changes.
 */
public class RestaurantAvailabilityChangedEvent extends DomainEvent {
    
    private final String restaurantId;
    private final boolean isAvailable;
    private final String reason;
    
    @JsonCreator
    public RestaurantAvailabilityChangedEvent(
            @JsonProperty("restaurantId") String restaurantId,
            @JsonProperty("isAvailable") boolean isAvailable,
            @JsonProperty("reason") String reason,
            @JsonProperty("version") int version) {
        super(restaurantId, version);
        this.restaurantId = restaurantId;
        this.isAvailable = isAvailable;
        this.reason = reason;
    }
    
    public String getRestaurantId() {
        return restaurantId;
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public String getEventType() {
        return "RestaurantAvailabilityChanged";
    }
    
    @Override
    public String toString() {
        return String.format("RestaurantAvailabilityChangedEvent{restaurantId='%s', isAvailable=%s, reason='%s', version=%d, occurredOn=%s}",
                restaurantId, isAvailable, reason, getVersion(), getOccurredOn());
    }
}