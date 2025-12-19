package com.restaurant.payment.domain.events;

import com.restaurant.events.DomainEvent;
import com.restaurant.payment.domain.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentInitiatedEvent extends DomainEvent {
    
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String paymentDetails;

    public PaymentInitiatedEvent() {
        super("", 1);
    }

    public PaymentInitiatedEvent(String paymentId, String orderId, String customerId, 
                               BigDecimal amount, PaymentMethod paymentMethod, String paymentDetails) {
        super(paymentId, 1);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDetails = paymentDetails;
    }

    @Override
    public String getEventType() {
        return "PaymentInitiated";
    }

    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentDetails() {
        return paymentDetails;
    }

    public void setPaymentDetails(String paymentDetails) {
        this.paymentDetails = paymentDetails;
    }
}