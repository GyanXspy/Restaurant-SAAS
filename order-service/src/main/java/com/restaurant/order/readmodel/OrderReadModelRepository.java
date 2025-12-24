package com.restaurant.order.readmodel;

import com.restaurant.order.model.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * MongoDB Repository for Order Read Model.
 * Uses MongoDB for optimized queries.
 */
@Repository
public interface OrderReadModelRepository extends MongoRepository<OrderReadModel, String> {
    
    List<OrderReadModel> findByCustomerId(String customerId);
    
    List<OrderReadModel> findByRestaurantId(String restaurantId);
    
    List<OrderReadModel> findByStatus(OrderStatus status);
}
