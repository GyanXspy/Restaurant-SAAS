package com.restaurant.order.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_read_model")
@Data
@NoArgsConstructor
public class OrderReadModelEntity {

    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Column(name = "restaurant_id", nullable = false)
    private String restaurantId;

    @Column(name = "restaurant_name")
    private String restaurantName;

    @Column(name = "items", columnDefinition = "JSON")
    private String items;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public OrderReadModelEntity(String id, String customerId, String restaurantId, 
                               String restaurantName, String items, String status, 
                               BigDecimal totalAmount, LocalDateTime createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.items = items;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = LocalDateTime.now();
    }

    public void setStatus(String status) {
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }
}