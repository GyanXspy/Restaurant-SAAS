package com.restaurant.order.command;

import jakarta.validation.constraints.NotBlank;

/**
 * Command to cancel an order with a specific reason.
 */
public class CancelOrderCommand {
    
    @NotBlank(message = "Order ID is required")
    private String orderId;
    
    @NotBlank(message = "Cancellation reason is required")
    private String reason;
    
    public CancelOrderCommand() {
    }
    
    public CancelOrderCommand(String orderId, String reason) {
        this.orderId = orderId;
        this.reason = reason;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}