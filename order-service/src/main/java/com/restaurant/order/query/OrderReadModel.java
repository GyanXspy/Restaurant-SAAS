package com.restaurant.order.query;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read model entity for Order queries.
 * Optimized for read operations and denormalized for query performance.
 * Updated asynchronously through domain event projections.
 */
@Entity
@Table(name = "order_read_model", indexes = {
    @Index(name = "idx_customer_id", columnList = "customerId"),
    @Index(name = "idx_restaurant_id", columnList = "restaurantId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderReadModel {
    
    @Id
    private String id;
    
    @Column(nullable = false)
    private String customerId;
    
    @Column(nullable = false)
    private String restaurantId;
    
    private String restaurantName;
    
    @Column(columnDefinition = "JSON")
    private String items; // JSON representation of order items
    
    @Column(nullable = false)
    private String status;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;
    
    private String paymentId;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}