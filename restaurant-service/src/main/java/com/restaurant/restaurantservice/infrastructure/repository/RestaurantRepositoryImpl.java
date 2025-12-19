package com.restaurant.restaurantservice.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.restaurant.restaurantservice.domain.model.Restaurant;
import com.restaurant.restaurantservice.domain.model.RestaurantId;
import com.restaurant.restaurantservice.domain.repository.RestaurantRepository;

/**
 * Implementation of RestaurantRepository using MongoDB.
 * Delegates to Spring Data MongoDB repository for persistence operations.
 */
@Component
public class RestaurantRepositoryImpl implements RestaurantRepository {
    
    private final MongoRestaurantRepository mongoRepository;

    public RestaurantRepositoryImpl(MongoRestaurantRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public Restaurant save(Restaurant restaurant) {
        return mongoRepository.save(restaurant);
    }

    @Override
    public Optional<Restaurant> findByRestaurantId(RestaurantId restaurantId) {
        return mongoRepository.findByRestaurantId(restaurantId);
    }

    @Override
    public Optional<Restaurant> findById(String id) {
        return mongoRepository.findById(id);
    }

    @Override
    public List<Restaurant> findAllActive() {
        return mongoRepository.findByIsActiveTrue();
    }

    @Override
    public List<Restaurant> findByCuisine(String cuisine) {
        return mongoRepository.findByCuisineIgnoreCase(cuisine);
    }

    @Override
    public List<Restaurant> findByCity(String city) {
        return mongoRepository.findByAddressCityIgnoreCase(city);
    }

    @Override
    public boolean existsByRestaurantId(RestaurantId restaurantId) {
        return mongoRepository.existsByRestaurantId(restaurantId);
    }

    @Override
    public void deleteByRestaurantId(RestaurantId restaurantId) {
        mongoRepository.deleteByRestaurantId(restaurantId);
    }

    @Override
    public long count() {
        return mongoRepository.count();
    }

    @Override
    public long countActive() {
        return mongoRepository.countByIsActiveTrue();
    }
}