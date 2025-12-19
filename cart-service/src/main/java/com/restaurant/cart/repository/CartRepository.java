package com.restaurant.cart.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.restaurant.cart.domain.Cart;
import com.restaurant.cart.domain.CartStatus;

/**
 * Repository interface for Cart aggregate operations.
 */
@Repository
public interface CartRepository extends MongoRepository<Cart, String> {

    /**
     * Find active cart by customer ID.
     */
    Optional<Cart> findByCustomerIdAndStatus(String customerId, CartStatus status);

    /**
     * Find all carts by customer ID.
     */
    List<Cart> findByCustomerId(String customerId);

    /**
     * Find expired carts that need cleanup.
     */
    @Query("{ 'expiresAt': { $lt: ?0 }, 'status': 'ACTIVE' }")
    List<Cart> findExpiredCarts(LocalDateTime currentTime);

    /**
     * Find carts by restaurant ID.
     */
    List<Cart> findByRestaurantIdAndStatus(String restaurantId, CartStatus status);

    /**
     * Check if customer has an active cart.
     */
    boolean existsByCustomerIdAndStatus(String customerId, CartStatus status);

    /**
     * Delete carts older than specified date.
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}