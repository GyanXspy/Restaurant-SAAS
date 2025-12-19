package com.restaurant.payment.domain;

import com.restaurant.events.DomainEvent;
import com.restaurant.payment.domain.events.PaymentCompletedEvent;
import com.restaurant.payment.domain.events.PaymentFailedEvent;
import com.restaurant.payment.domain.events.PaymentInitiatedEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Payment {
    
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String paymentDetails;
    private String transactionId;
    private String failureReason;
    private String errorCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<DomainEvent> uncommittedEvents = new ArrayList<>();
    private int version = 0;

    // Default constructor for event sourcing
    public Payment() {
    }

    // Constructor for creating new payment
    public Payment(String orderId, String customerId, BigDecimal amount, 
                  PaymentMethod paymentMethod, String paymentDetails) {
        this.paymentId = UUID.randomUUID().toString();
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDetails = paymentDetails;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        
        // Apply domain event
        PaymentInitiatedEvent event = new PaymentInitiatedEvent(
            paymentId, orderId, customerId, amount, paymentMethod, paymentDetails);
        applyEvent(event);
    }

    // Event sourcing: rebuild from events
    public static Payment fromEvents(List<DomainEvent> events) {
        Payment payment = new Payment();
        for (DomainEvent event : events) {
            payment.applyEvent(event);
            payment.version++;
        }
        payment.uncommittedEvents.clear();
        return payment;
    }

    // Business logic methods
    public void completePayment(String transactionId, String gatewayResponse) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Payment cannot be completed in current status: " + status);
        }
        
        PaymentCompletedEvent event = new PaymentCompletedEvent(
            paymentId, orderId, customerId, amount, transactionId, gatewayResponse);
        applyEvent(event);
    }

    public void failPayment(String failureReason, String errorCode, String gatewayResponse) {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Payment cannot be failed in current status: " + status);
        }
        
        PaymentFailedEvent event = new PaymentFailedEvent(
            paymentId, orderId, customerId, amount, failureReason, errorCode, gatewayResponse);
        applyEvent(event);
    }

    public void startProcessing() {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Payment cannot start processing in current status: " + status);
        }
        this.status = PaymentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    // Event application methods
    private void applyEvent(DomainEvent event) {
        if (event instanceof PaymentInitiatedEvent) {
            apply((PaymentInitiatedEvent) event);
        } else if (event instanceof PaymentCompletedEvent) {
            apply((PaymentCompletedEvent) event);
        } else if (event instanceof PaymentFailedEvent) {
            apply((PaymentFailedEvent) event);
        }
        
        uncommittedEvents.add(event);
    }

    private void apply(PaymentInitiatedEvent event) {
        this.paymentId = event.getPaymentId();
        this.orderId = event.getOrderId();
        this.customerId = event.getCustomerId();
        this.amount = event.getAmount();
        this.paymentMethod = event.getPaymentMethod();
        this.paymentDetails = event.getPaymentDetails();
        this.status = PaymentStatus.PENDING;
        this.createdAt = event.getOccurredOn();
        this.updatedAt = event.getOccurredOn();
    }

    private void apply(PaymentCompletedEvent event) {
        this.transactionId = event.getTransactionId();
        this.status = PaymentStatus.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    private void apply(PaymentFailedEvent event) {
        this.failureReason = event.getFailureReason();
        this.errorCode = event.getErrorCode();
        this.status = PaymentStatus.FAILED;
        this.updatedAt = LocalDateTime.now();
    }

    // Event sourcing helper methods
    public List<DomainEvent> getUncommittedEvents() {
        return new ArrayList<>(uncommittedEvents);
    }

    public void markEventsAsCommitted() {
        uncommittedEvents.clear();
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    // Validation methods
    public boolean isValidForProcessing() {
        return paymentId != null && 
               orderId != null && 
               customerId != null && 
               amount != null && 
               amount.compareTo(BigDecimal.ZERO) > 0 &&
               paymentMethod != null &&
               status == PaymentStatus.PENDING;
    }

    // Getters
    public String getPaymentId() {
        return paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}