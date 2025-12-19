package com.restaurant.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published to request payment processing as part of the order saga.
 */
public class PaymentInitiationRequestedEvent extends DomainEvent {
    
    private final String orderId;
    private final String customerId;
    private final BigDecimal amount;
    private final String paymentMethod;

    public PaymentInitiationRequestedEvent(String sagaId, String orderId, String customerId, 
                                         BigDecimal amount, String paymentMethod, int version) {
        super(sagaId, version);
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    @JsonCreator
    public PaymentInitiationRequestedEvent(@JsonProperty("eventId") String eventId,
                                         @JsonProperty("aggregateId") String aggregateId,
                                         @JsonProperty("occurredOn") LocalDateTime occurredOn,
                                         @JsonProperty("version") int version,
                                         @JsonProperty("orderId") String orderId,
                                         @JsonProperty("customerId") String customerId,
                                         @JsonProperty("amount") BigDecimal amount,
                                         @JsonProperty("paymentMethod") String paymentMethod) {
        super(eventId, aggregateId, occurredOn, version);
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }

    @Override
    public String getEventType() {
        return "PaymentInitiationRequested";
    }

    public String getOrderId() { return orderId; }
    public String getCustomerId() { return customerId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
}