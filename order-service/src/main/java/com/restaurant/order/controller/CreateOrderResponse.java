package com.restaurant.order.controller;

/**
 * Response for order creation requests.
 */
public class CreateOrderResponse {
    
    private String orderId;
    private String message;
    
    public CreateOrderResponse() {
    }
    
    public CreateOrderResponse(String orderId, String message) {
        this.orderId = orderId;
        this.message = message;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}