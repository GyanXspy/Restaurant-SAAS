package com.restaurant.order.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends OrderEvent {
    
    private String customerId;
    private String restaurantId;
    private BigDecimal totalAmount;
    private List<OrderItemData> items;
    
    public OrderCreatedEvent(String orderId, String customerId, String restaurantId, 
                            BigDecimal totalAmount, List<OrderItemData> items, int version) {
        super(orderId, version);
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalAmount = totalAmount;
        this.items = items;
    }
    
    // Constructor for Jackson deserialization
    @JsonCreator
    public OrderCreatedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("aggregateId") String orderId,
            @JsonProperty("occurredOn") LocalDateTime occurredOn,
            @JsonProperty("version") int version,
            @JsonProperty("customerId") String customerId,
            @JsonProperty("restaurantId") String restaurantId,
            @JsonProperty("totalAmount") BigDecimal totalAmount,
            @JsonProperty("items") List<OrderItemData> items) {
        super(eventId, orderId, occurredOn, version);
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.totalAmount = totalAmount;
        this.items = items;
    }
    
    @Override
    public String getEventType() {
        return "OrderCreated";
    }
    
    @Data
    public static class OrderItemData {
        private String menuItemId;
        private String name;
        private BigDecimal price;
        private Integer quantity;
        
        public OrderItemData() {}
        
        public OrderItemData(String menuItemId, String name, BigDecimal price, Integer quantity) {
            this.menuItemId = menuItemId;
            this.name = name;
            this.price = price;
            this.quantity = quantity;
        }
    }
}
