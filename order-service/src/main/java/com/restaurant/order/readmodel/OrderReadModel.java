package com.restaurant.order.readmodel;

import com.restaurant.order.model.OrderStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read Model for Order queries stored in MongoDB.
 * Optimized denormalized structure for fast reads.
 */
@Document(collection = "orders")
@Data
@NoArgsConstructor
public class OrderReadModel {
    
    @Id
    private String id;
    
    @Indexed
    private String customerId;
    
    @Indexed
    private String restaurantId;
    
    private BigDecimal totalAmount;
    
    @Indexed
    private OrderStatus status;
    
    private String paymentId;
    
    @Indexed
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    // Denormalized fields for faster queries
    private Integer itemCount;
    
    private String itemsSummary; // JSON or comma-separated summary
}
