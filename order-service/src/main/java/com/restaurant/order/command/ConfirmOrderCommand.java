package com.restaurant.order.command;

import jakarta.validation.constraints.NotBlank;

/**
 * Command to confirm an order after successful payment processing.
 */
public class ConfirmOrderCommand {
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotBlank(message = "Payment ID is required")
    private String paymentId;
    
    public ConfirmOrderCommand() {
    }
    
    public ConfirmOrderCommand(String orderId, String paymentId) {
        this.orderId = orderId;
        this.paymentId = paymentId;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getPaymentId() {
        return paymentId;
    }
    
    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }
}