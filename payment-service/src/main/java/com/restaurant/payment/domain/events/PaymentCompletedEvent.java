package com.restaurant.payment.domain.events;

import com.restaurant.events.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentCompletedEvent extends DomainEvent {
    
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String transactionId;
    private String gatewayResponse;

    public PaymentCompletedEvent() {
        super("", 1);
    }

    public PaymentCompletedEvent(String paymentId, String orderId, String customerId, 
                               BigDecimal amount, String transactionId, String gatewayResponse) {
        super(paymentId, 1);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.transactionId = transactionId;
        this.gatewayResponse = gatewayResponse;
    }

    @Override
    public String getEventType() {
        return "PaymentCompleted";
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

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }
}