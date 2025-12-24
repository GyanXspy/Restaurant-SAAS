package com.restaurant.order.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateOrderRequest {
    private String customerId;
    private String restaurantId;
    private List<OrderItemDto> items;
    private BigDecimal totalAmount;
}
