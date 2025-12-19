package com.restaurant.events;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published to request cart validation as part of the order saga.
 */
public class CartValidationRequestedEvent extends DomainEvent {
    
    private final String cartId;
    private final String customerId;
    private final String orderId;

    public CartValidationRequestedEvent(String sagaId, String cartId, String customerId, String orderId, int version) {
        super(sagaId, version);
        this.cartId = cartId;
        this.customerId = customerId;
        this.orderId = orderId;
    }

    @JsonCreator
    public CartValidationRequestedEvent(@JsonProperty("eventId") String eventId,
                                      @JsonProperty("aggregateId") String aggregateId,
                                      @JsonProperty("occurredOn") LocalDateTime occurredOn,
                                      @JsonProperty("version") int version,
                                      @JsonProperty("cartId") String cartId,
                                      @JsonProperty("customerId") String customerId,
                                      @JsonProperty("orderId") String orderId) {
        super(eventId, aggregateId, occurredOn, version);
        this.cartId = cartId;
        this.customerId = customerId;
        this.orderId = orderId;
    }

    @Override
    public String getEventType() {
        return "CartValidationRequested";
    }

    public String getCartId() { return cartId; }
    public String getCustomerId() { return customerId; }
    public String getOrderId() { return orderId; }
}