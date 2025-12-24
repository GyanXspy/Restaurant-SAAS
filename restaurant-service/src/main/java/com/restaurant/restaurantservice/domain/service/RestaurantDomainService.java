package com.restaurant.restaurantservice.domain.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.restaurantservice.domain.model.Restaurant;
import com.restaurant.restaurantservice.domain.model.RestaurantId;
import com.restaurant.restaurantservice.domain.repository.RestaurantRepository;

/**
 * Domain service for restaurant business operations.
 * Coordinates between repository and event publishing.
 */
@Service
@Transactional
public class RestaurantDomainService {
    
    private final RestaurantRepository restaurantRepository;
    private final EventPublisher eventPublisher;

    public RestaurantDomainService(RestaurantRepository restaurantRepository, EventPublisher eventPublisher) {
        this.restaurantRepository = restaurantRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates and saves a new restaurant, publishing domain events.
     */
    public Restaurant createRestaurant(Restaurant restaurant) {
        // Validate business rules
        validateUniqueRestaurantName(restaurant.getName(), restaurant.getAddress().getCity());
        
        // Save restaurant
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        
        // Publish domain events
        publishDomainEvents(savedRestaurant);
        //restaurant-events
        
        return savedRestaurant;
    }

    /**
     * Updates an existing restaurant and publishes domain events.
     */
    public Restaurant updateRestaurant(Restaurant restaurant) {
        // Verify restaurant exists
        if (!restaurantRepository.existsByRestaurantId(restaurant.getRestaurantId())) {
            throw new IllegalArgumentException("Restaurant not found: " + restaurant.getRestaurantId());
        }
        
        // Save restaurant
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);
        
        // Publish domain events
        publishDomainEvents(savedRestaurant);
        
        return savedRestaurant;
    }

    /**
     * Finds a restaurant by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Restaurant> findRestaurant(RestaurantId restaurantId) {
        return restaurantRepository.findByRestaurantId(restaurantId);
    }

    /**
     * Finds all active restaurants.
     */
    @Transactional(readOnly = true)
    public List<Restaurant> findActiveRestaurants() {
        return restaurantRepository.findAllActive();
    }

    /**
     * Finds restaurants by cuisine.
     */
    @Transactional(readOnly = true)
    public List<Restaurant> findRestaurantsByCuisine(String cuisine) {
        return restaurantRepository.findByCuisine(cuisine);
    }

    /**
     * Finds restaurants by city.
     */
    @Transactional(readOnly = true)
    public List<Restaurant> findRestaurantsByCity(String city) {
        return restaurantRepository.findByCity(city);
    }

    /**
     * Validates that restaurant name is unique within the same city.
     */
    private void validateUniqueRestaurantName(String name, String city) {
        List<Restaurant> existingRestaurants = restaurantRepository.findByCity(city);
        boolean nameExists = existingRestaurants.stream()
            .anyMatch(restaurant -> restaurant.getName().equalsIgnoreCase(name));
            
        if (nameExists) {
            throw new IllegalArgumentException(
                String.format("Restaurant with name '%s' already exists in city '%s'", name, city));
        }
    }

    /**
     * Publishes all domain events from the restaurant aggregate.
     */
    private void publishDomainEvents(Restaurant restaurant) {
        List<DomainEvent> events = restaurant.getDomainEvents();
        for (DomainEvent event : events) {
            eventPublisher.publish(event);
        }
        restaurant.clearDomainEvents();
    }
}