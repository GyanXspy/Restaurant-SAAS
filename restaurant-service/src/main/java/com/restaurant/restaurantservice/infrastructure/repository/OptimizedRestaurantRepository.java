package com.restaurant.restaurantservice.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.restaurant.restaurantservice.domain.model.Restaurant;
import com.restaurant.restaurantservice.domain.model.RestaurantId;

/**
 * Optimized Restaurant Repository with efficient query patterns.
 */
@Repository
public interface OptimizedRestaurantRepository extends MongoRepository<Restaurant, String> {
    
    /**
     * Find restaurant by domain ID with optimized query
     */
    @Query("{'restaurantId.value': ?0}")
    Optional<Restaurant> findByRestaurantIdOptimized(String restaurantId);
    
    /**
     * Find active restaurants with pagination
     */
    @Query(value = "{'isActive': true}", sort = "{'name': 1}")
    Page<Restaurant> findActiveRestaurantsWithPagination(Pageable pageable);
    
    /**
     * Find restaurants by cuisine and city with compound index
     */
    @Query("{'cuisine': ?0, 'address.city': ?1, 'isActive': true}")
    List<Restaurant> findByCuisineAndCityActive(String cuisine, String city);
    
    /**
     * Find restaurants by location with efficient projection
     */
    @Query(value = "{'address.city': ?0, 'isActive': true}", 
           fields = "{'restaurantId': 1, 'name': 1, 'cuisine': 1, 'address': 1, 'isActive': 1}")
    List<Restaurant> findByLocationProjected(String city);
    
    /**
     * Search restaurants by name pattern
     */
    @Query("{'name': {'$regex': ?0, '$options': 'i'}, 'isActive': true}")
    List<Restaurant> findByNamePatternActive(String namePattern);
    
    /**
     * Find restaurants with available menu items
     */
    @Query("{'menu.available': true, 'isActive': true}")
    List<Restaurant> findRestaurantsWithAvailableItems();
    
    /**
     * Find restaurants by menu item category
     */
    @Query("{'menu.category': ?0, 'menu.available': true, 'isActive': true}")
    List<Restaurant> findByMenuCategory(String category);
    
    /**
     * Count restaurants by cuisine
     */
    @Aggregation(pipeline = {
        "{'$match': {'isActive': true}}",
        "{'$group': {'_id': '$cuisine', 'count': {'$sum': 1}}}",
        "{'$sort': {'count': -1}}"
    })
    List<CuisineCount> getRestaurantCountByCuisine();
    
    /**
     * Find restaurants with menu items in price range
     */
    @Query("{'menu.price': {'$gte': ?0, '$lte': ?1}, 'menu.available': true, 'isActive': true}")
    List<Restaurant> findByMenuPriceRange(Double minPrice, Double maxPrice);
    
    /**
     * Get restaurant menu with availability filter
     */
    @Aggregation(pipeline = {
        "{'$match': {'restaurantId.value': ?0, 'isActive': true}}",
        "{'$project': {'restaurantId': 1, 'name': 1, 'menu': {'$filter': {'input': '$menu', 'cond': {'$eq': ['$$this.available', true]}}}}}",
        "{'$limit': 1}"
    })
    Optional<Restaurant> getRestaurantWithAvailableMenu(String restaurantId);
    
    /**
     * Find nearby restaurants (placeholder for geospatial queries)
     */
    @Query("{'address.city': ?0, 'isActive': true}")
    List<Restaurant> findNearbyRestaurants(String city, Pageable pageable);
    
    /**
     * Bulk operations for restaurant status
     */
    @Query("{'restaurantId.value': {'$in': ?0}}")
    List<Restaurant> findByRestaurantIdIn(List<String> restaurantIds);
    
    /**
     * Check existence efficiently
     */
    @Query(value = "{'restaurantId.value': ?0}", exists = true)
    boolean existsByRestaurantIdOptimized(String restaurantId);
    
    /**
     * Find restaurants updated after timestamp
     */
    @Query("{'updatedAt': {'$gte': ?0}, 'isActive': true}")
    List<Restaurant> findRecentlyUpdated(java.time.LocalDateTime since);
    
    /**
     * Inner interface for aggregation results
     */
    interface CuisineCount {
        String getId();
        Long getCount();
    }
}