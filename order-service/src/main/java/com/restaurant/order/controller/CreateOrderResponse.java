package com.restaurant.order.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response for order creation requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {
    
    private String orderId;
    private String message;
}