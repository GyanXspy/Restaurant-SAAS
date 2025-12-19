package com.restaurant.restaurantservice.domain.repository;

import java.util.List;
import java.util.Optional;

import com.restaurant.restaurantservice.domain.model.Restaurant;
import com.restaurant.restaurantservice.domain.model.RestaurantId;

/**
 * Repository interface for Restaurant aggregate.
 * Defines contract for restaurant persistence operations.
 */
public interface RestaurantRepository {
    
    /**
     * Saves a restaurant aggregate.
     * 
     * @param restaurant the restaurant to save
     * @return the saved restaurant
     */
    Restaurant save(Restaurant restaurant);
    
    /**
     * Finds a restaurant by its domain ID.
     * 
     * @param restaurantId the restaurant domain ID
     * @return optional containing the restaurant if found
     */
    Optional<Restaurant> findByRestaurantId(RestaurantId restaurantId);
    
    /**
     * Finds a restaurant by its MongoDB document ID.
     * 
     * @param id the MongoDB document ID
     * @return optional containing the restaurant if found
     */
    Optional<Restaurant> findById(String id);
    
    /**
     * Finds all active restaurants.
     * 
     * @return list of active restaurants
     */
    List<Restaurant> findAllActive();
    
    /**
     * Finds restaurants by cuisine type.
     * 
     * @param cuisine the cuisine type
     * @return list of restaurants with the specified cuisine
     */
    List<Restaurant> findByCuisine(String cuisine);
    
    /**
     * Finds restaurants by city.
     * 
     * @param city the city name
     * @return list of restaurants in the specified city
     */
    List<Restaurant> findByCity(String city);
    
    /**
     * Checks if a restaurant exists by its domain ID.
     * 
     * @param restaurantId the restaurant domain ID
     * @return true if restaurant exists, false otherwise
     */
    boolean existsByRestaurantId(RestaurantId restaurantId);
    
    /**
     * Deletes a restaurant by its domain ID.
     * 
     * @param restaurantId the restaurant domain ID
     */
    void deleteByRestaurantId(RestaurantId restaurantId);
    
    /**
     * Counts total number of restaurants.
     * 
     * @return total count of restaurants
     */
    long count();
    
    /**
     * Counts active restaurants.
     * 
     * @return count of active restaurants
     */
    long countActive();
}