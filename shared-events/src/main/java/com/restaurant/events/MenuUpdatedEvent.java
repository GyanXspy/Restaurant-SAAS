package com.restaurant.events;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when restaurant menu is updated (items added, removed, or modified).
 */
public class MenuUpdatedEvent extends DomainEvent {
    
    private final String restaurantId;
    private final List<String> updatedItemIds;
    private final String updateType; // ADDED, REMOVED, MODIFIED

    public MenuUpdatedEvent(String eventAggregateId, String restaurantId, List<String> updatedItemIds, 
                          String updateType, int version) {
        super(eventAggregateId, version);
        this.restaurantId = restaurantId;
        this.updatedItemIds = updatedItemIds;
        this.updateType = updateType;
    }

    @JsonCreator
    public MenuUpdatedEvent(@JsonProperty("eventId") String eventId,
                          @JsonProperty("aggregateId") String aggregateId,
                          @JsonProperty("occurredOn") LocalDateTime occurredOn,
                          @JsonProperty("version") int version,
                          @JsonProperty("restaurantId") String restaurantId,
                          @JsonProperty("updatedItemIds") List<String> updatedItemIds,
                          @JsonProperty("updateType") String updateType) {
        super(eventId, aggregateId, occurredOn, version);
        this.restaurantId = restaurantId;
        this.updatedItemIds = updatedItemIds;
        this.updateType = updateType;
    }

    @Override
    public String getEventType() {
        return "MenuUpdated";
    }

    public String getRestaurantId() { return restaurantId; }
    public List<String> getUpdatedItemIds() { return updatedItemIds; }
    public String getUpdateType() { return updateType; }
}