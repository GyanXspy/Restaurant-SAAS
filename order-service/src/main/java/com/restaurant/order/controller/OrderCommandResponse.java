package com.restaurant.order.controller;

/**
 * Generic response for order command operations.
 */
public class OrderCommandResponse {
    
    private String message;
    
    public OrderCommandResponse() {
    }
    
    public OrderCommandResponse(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}