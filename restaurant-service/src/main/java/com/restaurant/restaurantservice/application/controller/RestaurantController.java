package com.restaurant.restaurantservice.application.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurantservice.application.dto.CreateRestaurantRequest;
import com.restaurant.restaurantservice.application.dto.RestaurantDto;
import com.restaurant.restaurantservice.application.mapper.RestaurantMapper;
import com.restaurant.restaurantservice.domain.model.Restaurant;
import com.restaurant.restaurantservice.domain.model.RestaurantId;
import com.restaurant.restaurantservice.domain.service.RestaurantDomainService;

import jakarta.validation.Valid;

/**
 * REST controller for restaurant management operations.
 * Provides endpoints for creating, updating, and querying restaurants.
 */
@RestController
@RequestMapping("/api/restaurants")
@Validated
public class RestaurantController {
    
    private final RestaurantDomainService restaurantDomainService;
    private final RestaurantMapper restaurantMapper;

    public RestaurantController(RestaurantDomainService restaurantDomainService, RestaurantMapper restaurantMapper) {
        this.restaurantDomainService = restaurantDomainService;
        this.restaurantMapper = restaurantMapper;
    }

    /**
     * Creates a new restaurant.
     */
    @PostMapping
    public ResponseEntity<RestaurantDto> createRestaurant(@Valid @RequestBody CreateRestaurantRequest request) {
        try {
            Restaurant restaurant = restaurantMapper.toRestaurant(request);
            Restaurant savedRestaurant = restaurantDomainService.createRestaurant(restaurant);
            RestaurantDto responseDto = restaurantMapper.toRestaurantDto(savedRestaurant);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Gets a restaurant by ID.
     */
    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantDto> getRestaurant(@PathVariable String restaurantId) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurant = restaurantDomainService.findRestaurant(id);
            
            if (restaurant.isPresent()) {
                RestaurantDto responseDto = restaurantMapper.toRestaurantDto(restaurant.get());
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Gets all active restaurants.
     */
    @GetMapping
    public ResponseEntity<List<RestaurantDto>> getActiveRestaurants() {
        List<Restaurant> restaurants = restaurantDomainService.findActiveRestaurants();
        List<RestaurantDto> responseDtos = restaurantMapper.toRestaurantDtos(restaurants);
        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Gets restaurants by cuisine type.
     */
    @GetMapping("/cuisine/{cuisine}")
    public ResponseEntity<List<RestaurantDto>> getRestaurantsByCuisine(@PathVariable String cuisine) {
        List<Restaurant> restaurants = restaurantDomainService.findRestaurantsByCuisine(cuisine);
        List<RestaurantDto> responseDtos = restaurantMapper.toRestaurantDtos(restaurants);
        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Gets restaurants by city.
     */
    @GetMapping("/city/{city}")
    public ResponseEntity<List<RestaurantDto>> getRestaurantsByCity(@PathVariable String city) {
        List<Restaurant> restaurants = restaurantDomainService.findRestaurantsByCity(city);
        List<RestaurantDto> responseDtos = restaurantMapper.toRestaurantDtos(restaurants);
        return ResponseEntity.ok(responseDtos);
    }

    /**
     * Activates a restaurant.
     */
    @PutMapping("/{restaurantId}/activate")
    public ResponseEntity<RestaurantDto> activateRestaurant(@PathVariable String restaurantId) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            restaurant.activate();
            Restaurant updatedRestaurant = restaurantDomainService.updateRestaurant(restaurant);
            RestaurantDto responseDto = restaurantMapper.toRestaurantDto(updatedRestaurant);
            
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Deactivates a restaurant.
     */
    @PutMapping("/{restaurantId}/deactivate")
    public ResponseEntity<RestaurantDto> deactivateRestaurant(@PathVariable String restaurantId) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            restaurant.deactivate();
            Restaurant updatedRestaurant = restaurantDomainService.updateRestaurant(restaurant);
            RestaurantDto responseDto = restaurantMapper.toRestaurantDto(updatedRestaurant);
            
            return ResponseEntity.ok(responseDto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}