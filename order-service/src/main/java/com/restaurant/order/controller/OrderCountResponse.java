package com.restaurant.order.controller;

/**
 * Response for order count queries.
 */
public class OrderCountResponse {
    
    private String restaurantId;
    private String status;
    private long count;
    
    public OrderCountResponse() {
    }
    
    public OrderCountResponse(String restaurantId, String status, long count) {
        this.restaurantId = restaurantId;
        this.status = status;
        this.count = count;
    }
    
    public String getRestaurantId() {
        return restaurantId;
    }
    
    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public long getCount() {
        return count;
    }
    
    public void setCount(long count) {
        this.count = count;
    }
}