package com.restaurant.restaurantservice.application.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.restaurantservice.application.dto.MenuItemDto;
import com.restaurant.restaurantservice.application.mapper.RestaurantMapper;
import com.restaurant.restaurantservice.domain.model.MenuItem;
import com.restaurant.restaurantservice.domain.model.Restaurant;
import com.restaurant.restaurantservice.domain.model.RestaurantId;
import com.restaurant.restaurantservice.domain.service.RestaurantDomainService;

import jakarta.validation.Valid;

/**
 * REST controller for menu management operations.
 * Provides endpoints for managing restaurant menu items.
 */
@RestController
@RequestMapping("/api/restaurants/{restaurantId}/menu")
@Validated
public class MenuController {
    
    private final RestaurantDomainService restaurantDomainService;
    private final RestaurantMapper restaurantMapper;

    public MenuController(RestaurantDomainService restaurantDomainService, RestaurantMapper restaurantMapper) {
        this.restaurantDomainService = restaurantDomainService;
        this.restaurantMapper = restaurantMapper;
    }

    /**
     * Gets all menu items for a restaurant.
     */
    @GetMapping
    public ResponseEntity<List<MenuItemDto>> getMenu(@PathVariable String restaurantId) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            List<MenuItemDto> menuItems = restaurant.getMenu().stream()
                .map(restaurantMapper::toMenuItemDto)
                .toList();
                
            return ResponseEntity.ok(menuItems);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Gets available menu items for a restaurant.
     */
    @GetMapping("/available")
    public ResponseEntity<List<MenuItemDto>> getAvailableMenu(@PathVariable String restaurantId) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            List<MenuItemDto> availableItems = restaurant.getAvailableMenuItems().stream()
                .map(restaurantMapper::toMenuItemDto)
                .toList();
                
            return ResponseEntity.ok(availableItems);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Gets menu items by category.
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<MenuItemDto>> getMenuByCategory(
            @PathVariable String restaurantId, 
            @PathVariable String category) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            List<MenuItemDto> categoryItems = restaurant.getMenuItemsByCategory(category).stream()
                .map(restaurantMapper::toMenuItemDto)
                .toList();
                
            return ResponseEntity.ok(categoryItems);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Gets a specific menu item.
     */
    @GetMapping("/{itemId}")
    public ResponseEntity<MenuItemDto> getMenuItem(
            @PathVariable String restaurantId, 
            @PathVariable String itemId) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            Optional<MenuItem> menuItemOpt = restaurant.findMenuItem(itemId);
            
            if (menuItemOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            MenuItemDto menuItemDto = restaurantMapper.toMenuItemDto(menuItemOpt.get());
            return ResponseEntity.ok(menuItemDto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Adds a new menu item to a restaurant.
     */
    @PostMapping
    public ResponseEntity<MenuItemDto> addMenuItem(
            @PathVariable String restaurantId,
            @Valid @RequestBody MenuItemDto menuItemDto) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            MenuItem menuItem = restaurantMapper.toMenuItem(menuItemDto);
            restaurant.addMenuItem(menuItem);
            
            Restaurant updatedRestaurant = restaurantDomainService.updateRestaurant(restaurant);
            Optional<MenuItem> addedItem = updatedRestaurant.findMenuItem(menuItem.getItemId());
            
            if (addedItem.isPresent()) {
                MenuItemDto responseDto = restaurantMapper.toMenuItemDto(addedItem.get());
                return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
            } else {
                return ResponseEntity.internalServerError().build();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Updates an existing menu item.
     */
    @PutMapping("/{itemId}")
    public ResponseEntity<MenuItemDto> updateMenuItem(
            @PathVariable String restaurantId,
            @PathVariable String itemId,
            @Valid @RequestBody MenuItemDto menuItemDto) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            
            // Ensure the item ID matches
            menuItemDto.setItemId(itemId);
            MenuItem updatedMenuItem = restaurantMapper.toMenuItem(menuItemDto);
            restaurant.updateMenuItem(updatedMenuItem);
            
            Restaurant updatedRestaurant = restaurantDomainService.updateRestaurant(restaurant);
            Optional<MenuItem> updatedItem = updatedRestaurant.findMenuItem(itemId);
            
            if (updatedItem.isPresent()) {
                MenuItemDto responseDto = restaurantMapper.toMenuItemDto(updatedItem.get());
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.internalServerError().build();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Updates menu item availability.
     */
    @PutMapping("/{itemId}/availability")
    public ResponseEntity<MenuItemDto> updateItemAvailability(
            @PathVariable String restaurantId,
            @PathVariable String itemId,
            @RequestParam boolean available) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            restaurant.updateItemAvailability(itemId, available);
            
            Restaurant updatedRestaurant = restaurantDomainService.updateRestaurant(restaurant);
            Optional<MenuItem> updatedItem = updatedRestaurant.findMenuItem(itemId);
            
            if (updatedItem.isPresent()) {
                MenuItemDto responseDto = restaurantMapper.toMenuItemDto(updatedItem.get());
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Updates menu item price.
     */
    @PutMapping("/{itemId}/price")
    public ResponseEntity<MenuItemDto> updateItemPrice(
            @PathVariable String restaurantId,
            @PathVariable String itemId,
            @RequestParam BigDecimal price) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            restaurant.updateItemPrice(itemId, price);
            
            Restaurant updatedRestaurant = restaurantDomainService.updateRestaurant(restaurant);
            Optional<MenuItem> updatedItem = updatedRestaurant.findMenuItem(itemId);
            
            if (updatedItem.isPresent()) {
                MenuItemDto responseDto = restaurantMapper.toMenuItemDto(updatedItem.get());
                return ResponseEntity.ok(responseDto);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Removes a menu item from a restaurant.
     */
    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeMenuItem(
            @PathVariable String restaurantId,
            @PathVariable String itemId) {
        try {
            RestaurantId id = RestaurantId.of(restaurantId);
            Optional<Restaurant> restaurantOpt = restaurantDomainService.findRestaurant(id);
            
            if (restaurantOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Restaurant restaurant = restaurantOpt.get();
            restaurant.removeMenuItem(itemId);
            
            restaurantDomainService.updateRestaurant(restaurant);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }
}