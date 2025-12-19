package com.restaurant.events;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when cart validation is completed, either successfully or with errors.
 */
public class CartValidationCompletedEvent extends DomainEvent {
    
    private final String cartId;
    private final String orderId;
    private final boolean isValid;
    private final List<String> validationErrors;

    public CartValidationCompletedEvent(String sagaId, String cartId, String orderId, 
                                      boolean isValid, List<String> validationErrors, int version) {
        super(sagaId, version);
        this.cartId = cartId;
        this.orderId = orderId;
        this.isValid = isValid;
        this.validationErrors = validationErrors;
    }

    @JsonCreator
    public CartValidationCompletedEvent(@JsonProperty("eventId") String eventId,
                                      @JsonProperty("aggregateId") String aggregateId,
                                      @JsonProperty("occurredOn") LocalDateTime occurredOn,
                                      @JsonProperty("version") int version,
                                      @JsonProperty("cartId") String cartId,
                                      @JsonProperty("orderId") String orderId,
                                      @JsonProperty("isValid") boolean isValid,
                                      @JsonProperty("validationErrors") List<String> validationErrors) {
        super(eventId, aggregateId, occurredOn, version);
        this.cartId = cartId;
        this.orderId = orderId;
        this.isValid = isValid;
        this.validationErrors = validationErrors;
    }

    @Override
    public String getEventType() {
        return "CartValidationCompleted";
    }

    public String getCartId() { return cartId; }
    public String getOrderId() { return orderId; }
    public boolean isValid() { return isValid; }
    public List<String> getValidationErrors() { return validationErrors; }
}