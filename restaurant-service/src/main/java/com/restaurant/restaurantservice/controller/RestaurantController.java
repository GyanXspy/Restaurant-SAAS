package com.restaurant.restaurantservice.controller;

import com.restaurant.restaurantservice.domain.model.Address;
import com.restaurant.restaurantservice.domain.model.MenuItem;
import com.restaurant.restaurantservice.domain.model.Restaurant;
import com.restaurant.restaurantservice.domain.model.RestaurantId;
import com.restaurant.restaurantservice.domain.service.RestaurantDomainService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/restaurants")
public class RestaurantController {

    private final RestaurantDomainService restaurantDomainService;

    @Autowired
    public RestaurantController(RestaurantDomainService restaurantDomainService) {
        this.restaurantDomainService = restaurantDomainService;
    }

    @PostMapping
    public ResponseEntity<RestaurantResponse> createRestaurant(@RequestBody CreateRestaurantRequest request) {
        try {
            Address address = new Address(
                request.getStreet(),
                request.getCity(),
                request.getZipCode(),
                request.getCountry()
            );
            
            Restaurant restaurant = new Restaurant(
                request.getName(),
                request.getCuisine(),
                address
            );
            
            Restaurant savedRestaurant = restaurantDomainService.createRestaurant(restaurant);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(RestaurantResponse.fromRestaurant(savedRestaurant));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{restaurantId}")
    public ResponseEntity<RestaurantResponse> getRestaurant(@PathVariable String restaurantId) {
        Optional<Restaurant> restaurant = restaurantDomainService.findRestaurant(RestaurantId.of(restaurantId));
        
        return restaurant
            .map(r -> ResponseEntity.ok(RestaurantResponse.fromRestaurant(r)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<RestaurantResponse>> getActiveRestaurants() {
        List<Restaurant> restaurants = restaurantDomainService.findActiveRestaurants();
        List<RestaurantResponse> response = restaurants.stream()
            .map(RestaurantResponse::fromRestaurant)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cuisine/{cuisine}")
    public ResponseEntity<List<RestaurantResponse>> getRestaurantsByCuisine(@PathVariable String cuisine) {
        List<Restaurant> restaurants = restaurantDomainService.findRestaurantsByCuisine(cuisine);
        List<RestaurantResponse> response = restaurants.stream()
            .map(RestaurantResponse::fromRestaurant)
            .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/city/{city}")
    public ResponseEntity<List<RestaurantResponse>> getRestaurantsByCity(@PathVariable String city) {
        List<Restaurant> restaurants = restaurantDomainService.findRestaurantsByCity(city);
        List<RestaurantResponse> response = restaurants.stream()
            .map(RestaurantResponse::fromRestaurant)
            .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{restaurantId}/menu")
    public ResponseEntity<String> addMenuItem(
            @PathVariable String restaurantId,
            @RequestBody AddMenuItemRequest request) {
        try {
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(RestaurantId.of(restaurantId));
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            MenuItem menuItem = new MenuItem(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getCategory(),
                request.isAvailable()
            );
            
            restaurant.addMenuItem(menuItem);
            restaurantDomainService.updateRestaurant(restaurant);
            
            return ResponseEntity.ok("Menu item added successfully");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{restaurantId}/activate")
    public ResponseEntity<String> activateRestaurant(@PathVariable String restaurantId) {
        try {
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(RestaurantId.of(restaurantId));
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            restaurant.activate();
            restaurantDomainService.updateRestaurant(restaurant);
            
            return ResponseEntity.ok("Restaurant activated");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{restaurantId}/deactivate")
    public ResponseEntity<String> deactivateRestaurant(@PathVariable String restaurantId) {
        try {
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(RestaurantId.of(restaurantId));
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            restaurant.deactivate();
            restaurantDomainService.updateRestaurant(restaurant);
            
            return ResponseEntity.ok("Restaurant deactivated");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DTOs
    @Data
    public static class CreateRestaurantRequest {
        private String name;
        private String cuisine;
        private String street;
        private String city;
        private String zipCode;
        private String country;
    }

    @Data
    public static class AddMenuItemRequest {
        private String name;
        private String description;
        private BigDecimal price;
        private String category;
        private boolean available;
    }

    @Data
    public static class RestaurantResponse {
        private String id;
        private String restaurantId;
        private String name;
        private String cuisine;
        private String address;
        private boolean active;
        private int menuItemCount;

        public static RestaurantResponse fromRestaurant(Restaurant restaurant) {
            RestaurantResponse response = new RestaurantResponse();
            response.id = restaurant.getId();
            response.restaurantId = restaurant.getRestaurantId().getValue();
            response.name = restaurant.getName();
            response.cuisine = restaurant.getCuisine();
            response.address = restaurant.getAddress().getFormattedAddress();
            response.active = restaurant.isActive();
            response.menuItemCount = restaurant.getMenu().size();
            return response;
        }
    }
}
