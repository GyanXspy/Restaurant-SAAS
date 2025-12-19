package com.restaurant.cart.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.restaurant.cart.domain.Cart;

/**
 * Optimized Cart Repository with efficient query patterns.
 */
@Repository
public interface OptimizedCartRepository extends MongoRepository<Cart, String> {
    
    /**
     * Find cart by customer and restaurant with compound index
     */
    @Query("{'customerId': ?0, 'restaurantId': ?1}")
    Optional<Cart> findByCustomerAndRestaurant(String customerId, String restaurantId);
    
    /**
     * Find active carts for customer with sorting
     */
    @Query(value = "{'customerId': ?0, 'expiresAt': {'$gt': ?1}}", sort = "{'updatedAt': -1}")
    List<Cart> findActiveCartsByCustomer(String customerId, LocalDateTime currentTime);
    
    /**
     * Find cart by ID with item projection
     */
    @Query(value = "{'cartId': ?0}", fields = "{'cartId': 1, 'customerId': 1, 'restaurantId': 1, 'items': 1, 'totalAmount': 1, 'updatedAt': 1}")
    Optional<Cart> findByCartIdProjected(String cartId);
    
    /**
     * Find carts by restaurant for analytics
     */
    @Query(value = "{'restaurantId': ?0, 'items': {'$exists': true, '$not': {'$size': 0}}}")
    List<Cart> findNonEmptyCartsByRestaurant(String restaurantId);
    
    /**
     * Find expired carts for cleanup
     */
    @Query("{'expiresAt': {'$lt': ?0}}")
    List<Cart> findExpiredCarts(LocalDateTime currentTime);
    
    /**
     * Find carts with specific item
     */
    @Query("{'items.itemId': ?0}")
    List<Cart> findCartsContainingItem(String itemId);
    
    /**
     * Count active carts by restaurant
     */
    @Aggregation(pipeline = {
        "{'$match': {'expiresAt': {'$gt': ?0}, 'items': {'$exists': true, '$not': {'$size': 0}}}}",
        "{'$group': {'_id': '$restaurantId', 'count': {'$sum': 1}, 'totalValue': {'$sum': '$totalAmount'}}}",
        "{'$sort': {'count': -1}}"
    })
    List<RestaurantCartStats> getCartStatsByRestaurant(LocalDateTime currentTime);
    
    /**
     * Find carts by total amount range
     */
    @Query("{'totalAmount': {'$gte': ?0, '$lte': ?1}, 'expiresAt': {'$gt': ?2}}")
    List<Cart> findByTotalAmountRange(Double minAmount, Double maxAmount, LocalDateTime currentTime);
    
    /**
     * Find recently updated carts
     */
    @Query(value = "{'updatedAt': {'$gte': ?0}}", sort = "{'updatedAt': -1}")
    Page<Cart> findRecentlyUpdated(LocalDateTime since, Pageable pageable);
    
    /**
     * Bulk find carts by customer IDs
     */
    @Query("{'customerId': {'$in': ?0}, 'expiresAt': {'$gt': ?1}}")
    List<Cart> findActiveCartsByCustomerIds(List<String> customerIds, LocalDateTime currentTime);
    
    /**
     * Find carts with high item count
     */
    @Query("{'items.10': {'$exists': true}}")  // Carts with more than 10 items
    List<Cart> findLargeCarts();
    
    /**
     * Count carts by customer
     */
    @Query(value = "{'customerId': ?0, 'expiresAt': {'$gt': ?1}}", count = true)
    long countActiveCartsByCustomer(String customerId, LocalDateTime currentTime);
    
    /**
     * Find abandoned carts (not updated recently)
     */
    @Query("{'updatedAt': {'$lt': ?0}, 'expiresAt': {'$gt': ?1}}")
    List<Cart> findAbandonedCarts(LocalDateTime abandonedThreshold, LocalDateTime currentTime);
    
    /**
     * Delete expired carts efficiently
     */
    @Query(value = "{'expiresAt': {'$lt': ?0}}", delete = true)
    long deleteExpiredCarts(LocalDateTime currentTime);
    
    /**
     * Check if customer has active cart for restaurant
     */
    @Query(value = "{'customerId': ?0, 'restaurantId': ?1, 'expiresAt': {'$gt': ?2}}", exists = true)
    boolean hasActiveCartForRestaurant(String customerId, String restaurantId, LocalDateTime currentTime);
    
    /**
     * Inner interface for aggregation results
     */
    interface RestaurantCartStats {
        String getId();
        Long getCount();
        Double getTotalValue();
    }
}