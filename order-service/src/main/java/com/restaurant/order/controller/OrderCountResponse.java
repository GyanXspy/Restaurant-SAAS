package com.restaurant.order.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for order count queries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCountResponse {
    
    private String restaurantId;
    private String status;
    private long count;
}