package com.restaurant.events;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when a new restaurant is registered in the system.
 */
public class RestaurantCreatedEvent extends DomainEvent {
    
    private final String name;
    private final String cuisine;
    private final String address;

    public RestaurantCreatedEvent(String restaurantId, String name, String cuisine, String address, int version) {
        super(restaurantId, version);
        this.name = name;
        this.cuisine = cuisine;
        this.address = address;
    }

    @JsonCreator
    public RestaurantCreatedEvent(@JsonProperty("eventId") String eventId,
                                @JsonProperty("aggregateId") String aggregateId,
                                @JsonProperty("occurredOn") LocalDateTime occurredOn,
                                @JsonProperty("version") int version,
                                @JsonProperty("name") String name,
                                @JsonProperty("cuisine") String cuisine,
                                @JsonProperty("address") String address) {
        super(eventId, aggregateId, occurredOn, version);
        this.name = name;
        this.cuisine = cuisine;
        this.address = address;
    }

    @Override
    public String getEventType() {
        return "RestaurantCreated";
    }

    public String getName() { return name; }
    public String getCuisine() { return cuisine; }
    public String getAddress() { return address; }
}