package com.restaurant.restaurantservice.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import com.restaurant.restaurantservice.domain.model.Restaurant;
import com.restaurant.restaurantservice.domain.model.RestaurantId;

/**
 * Repository interface for Restaurant aggregate.
 * Spring Data MongoDB repository with custom query methods.
 */
@Repository
public interface RestaurantRepository extends MongoRepository<Restaurant, String> {
    
    /**
     * Finds a restaurant by its domain ID.
     */
    Optional<Restaurant> findByRestaurantId(RestaurantId restaurantId);
    
    /**
     * Finds all active restaurants.
     */
    @Query("{ 'isActive': true }")
    List<Restaurant> findAllActive();
    
    /**
     * Finds restaurants by cuisine type.
     */
    List<Restaurant> findByCuisine(String cuisine);
    
    /**
     * Finds restaurants by city.
     */
    @Query("{ 'address.city': ?0 }")
    List<Restaurant> findByCity(String city);
    
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
    @Query(value = "{ 'isActive': true }", count = true)
    long countActive();
}