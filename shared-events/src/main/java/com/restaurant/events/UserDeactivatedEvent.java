package com.restaurant.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Event published when a user is deactivated in the system.
 */
public class UserDeactivatedEvent extends DomainEvent {
    
    private final String email;
    private final String reason;

    public UserDeactivatedEvent(String userId, String email, String reason, int version) {
        super(userId, version);
        this.email = email;
        this.reason = reason;
    }

    @JsonCreator
    public UserDeactivatedEvent(@JsonProperty("eventId") String eventId,
                               @JsonProperty("aggregateId") String aggregateId,
                               @JsonProperty("occurredOn") LocalDateTime occurredOn,
                               @JsonProperty("version") int version,
                               @JsonProperty("email") String email,
                               @JsonProperty("reason") String reason) {
        super(eventId, aggregateId, occurredOn, version);
        this.email = email;
        this.reason = reason;
    }

    @Override
    public String getEventType() {
        return "UserDeactivated";
    }

    public String getEmail() { return email; }
    public String getReason() { return reason; }
}