package com.restaurant.order.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for OrderReadModel queries.
 * Provides optimized query methods for the read side of CQRS.
 */
@Repository
public interface OrderReadModelRepository extends JpaRepository<OrderReadModel, String> {
    
    /**
     * Find orders by customer ID with pagination.
     */
    Page<OrderReadModel> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
    
    /**
     * Find orders by restaurant ID with pagination.
     */
    Page<OrderReadModel> findByRestaurantIdOrderByCreatedAtDesc(String restaurantId, Pageable pageable);
    
    /**
     * Find orders by status with pagination.
     */
    Page<OrderReadModel> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
    
    /**
     * Find orders by customer ID and status.
     */
    Page<OrderReadModel> findByCustomerIdAndStatusOrderByCreatedAtDesc(
        String customerId, String status, Pageable pageable);
    
    /**
     * Find orders by restaurant ID and status.
     */
    Page<OrderReadModel> findByRestaurantIdAndStatusOrderByCreatedAtDesc(
        String restaurantId, String status, Pageable pageable);
    
    /**
     * Find recent orders for a customer (last 10).
     */
    List<OrderReadModel> findTop10ByCustomerIdOrderByCreatedAtDesc(String customerId);
    
    /**
     * Find active orders for a restaurant (PENDING and CONFIRMED status).
     */
    @Query("SELECT o FROM OrderReadModel o WHERE o.restaurantId = :restaurantId " +
           "AND o.status IN ('PENDING', 'CONFIRMED') ORDER BY o.createdAt DESC")
    List<OrderReadModel> findActiveOrdersByRestaurantId(@Param("restaurantId") String restaurantId);
    
    /**
     * Count orders by status for a restaurant.
     */
    @Query("SELECT COUNT(o) FROM OrderReadModel o WHERE o.restaurantId = :restaurantId AND o.status = :status")
    long countByRestaurantIdAndStatus(@Param("restaurantId") String restaurantId, @Param("status") String status);
    
    /**
     * Find order by payment ID.
     */
    Optional<OrderReadModel> findByPaymentId(String paymentId);
}