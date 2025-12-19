package com.restaurant.order.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic response for order command operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCommandResponse {
    
    private String message;
}