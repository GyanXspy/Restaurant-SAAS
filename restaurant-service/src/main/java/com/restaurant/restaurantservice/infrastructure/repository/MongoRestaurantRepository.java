package com.restaurant.restaurantservice.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.restaurant.restaurantservice.domain.model.Restaurant;
import com.restaurant.restaurantservice.domain.model.RestaurantId;

/**
 * Spring Data MongoDB repository for Restaurant aggregate.
 * Provides MongoDB-specific query methods and implementations.
 */
@Repository
public interface MongoRestaurantRepository extends MongoRepository<Restaurant, String> {
    
    /**
     * Finds a restaurant by its domain ID.
     */
    Optional<Restaurant> findByRestaurantId(RestaurantId restaurantId);
    
    /**
     * Finds all active restaurants.
     */
    List<Restaurant> findByIsActiveTrue();
    
    /**
     * Finds restaurants by cuisine type (case-insensitive).
     */
    @Query("{'cuisine': {$regex: ?0, $options: 'i'}}")
    List<Restaurant> findByCuisineIgnoreCase(String cuisine);
    
    /**
     * Finds restaurants by city (case-insensitive).
     */
    @Query("{'address.city': {$regex: ?0, $options: 'i'}}")
    List<Restaurant> findByAddressCityIgnoreCase(String city);
    
    /**
     * Checks if a restaurant exists by its domain ID.
     */
    boolean existsByRestaurantId(RestaurantId restaurantId);
    
    /**
     * Deletes a restaurant by its domain ID.
     */
    void deleteByRestaurantId(RestaurantId restaurantId);
    
    /**
     * Counts active restaurants.
     */
    long countByIsActiveTrue();
    
    /**
     * Finds restaurants by name containing the given text (case-insensitive).
     */
    @Query("{'name': {$regex: ?0, $options: 'i'}}")
    List<Restaurant> findByNameContainingIgnoreCase(String name);
    
    /**
     * Finds restaurants that have menu items in a specific category.
     */
    @Query("{'menu.category': {$regex: ?0, $options: 'i'}}")
    List<Restaurant> findByMenuCategory(String category);
}