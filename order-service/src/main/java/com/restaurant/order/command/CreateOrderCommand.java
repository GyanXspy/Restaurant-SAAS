package com.restaurant.order.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderCommand {
    private String customerId;
    private String restaurantId;
    private BigDecimal totalAmount;
    private List<OrderItemCommand> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemCommand {
        private String menuItemId;
        private String name;
        private BigDecimal price;
        private Integer quantity;
    }
}
