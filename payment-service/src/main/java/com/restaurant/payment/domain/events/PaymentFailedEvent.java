package com.restaurant.payment.domain.events;

import com.restaurant.events.DomainEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentFailedEvent extends DomainEvent {
    
    private String paymentId;
    private String orderId;
    private String customerId;
    private BigDecimal amount;
    private String failureReason;
    private String errorCode;
    private String gatewayResponse;

    public PaymentFailedEvent() {
        super("", 1);
    }

    public PaymentFailedEvent(String paymentId, String orderId, String customerId, 
                            BigDecimal amount, String failureReason, String errorCode, String gatewayResponse) {
        super(paymentId, 1);
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.customerId = customerId;
        this.amount = amount;
        this.failureReason = failureReason;
        this.errorCode = errorCode;
        this.gatewayResponse = gatewayResponse;
    }

    @Override
    public String getEventType() {
        return "PaymentFailed";
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

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getGatewayResponse() {
        return gatewayResponse;
    }

    public void setGatewayResponse(String gatewayResponse) {
        this.gatewayResponse = gatewayResponse;
    }
}