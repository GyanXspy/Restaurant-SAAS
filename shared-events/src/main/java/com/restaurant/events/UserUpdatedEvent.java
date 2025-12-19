package com.restaurant.events;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when user profile information is updated.
 */
public class UserUpdatedEvent extends DomainEvent {
    
    private final String email;
    private final String firstName;
    private final String lastName;

    public UserUpdatedEvent(String userId, String email, String firstName, String lastName, int version) {
        super(userId, version);
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @JsonCreator
    public UserUpdatedEvent(@JsonProperty("eventId") String eventId,
                          @JsonProperty("aggregateId") String aggregateId,
                          @JsonProperty("occurredOn") LocalDateTime occurredOn,
                          @JsonProperty("version") int version,
                          @JsonProperty("email") String email,
                          @JsonProperty("firstName") String firstName,
                          @JsonProperty("lastName") String lastName) {
        super(eventId, aggregateId, occurredOn, version);
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String getEventType() {
        return "UserUpdated";
    }

    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}