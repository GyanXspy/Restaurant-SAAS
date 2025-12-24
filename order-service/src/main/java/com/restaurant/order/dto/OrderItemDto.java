package com.restaurant.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDto {
    private String menuItemId;
    private String name;
    private BigDecimal price;
    private Integer quantity;
}
