package com.restaurant.events;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Event published when payment processing is completed, either successfully or with failure.
 */
public class PaymentProcessingCompletedEvent extends DomainEvent {
    
    private final String paymentId;
    private final String orderId;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final String failureReason;

    public PaymentProcessingCompletedEvent(String sagaId, String paymentId, String orderId, 
                                         BigDecimal amount, PaymentStatus status, String failureReason, int version) {
        super(sagaId, version);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
    }

    @JsonCreator
    public PaymentProcessingCompletedEvent(@JsonProperty("eventId") String eventId,
                                         @JsonProperty("aggregateId") String aggregateId,
                                         @JsonProperty("occurredOn") LocalDateTime occurredOn,
                                         @JsonProperty("version") int version,
                                         @JsonProperty("paymentId") String paymentId,
                                         @JsonProperty("orderId") String orderId,
                                         @JsonProperty("amount") BigDecimal amount,
                                         @JsonProperty("status") PaymentStatus status,
                                         @JsonProperty("failureReason") String failureReason) {
        super(eventId, aggregateId, occurredOn, version);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
    }

    @Override
    public String getEventType() {
        return "PaymentProcessingCompleted";
    }

    public String getPaymentId() { return paymentId; }
    public String getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }

    public enum PaymentStatus {
        COMPLETED, FAILED, TIMEOUT
    }
}