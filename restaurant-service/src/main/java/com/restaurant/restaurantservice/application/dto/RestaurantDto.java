package com.restaurant.restaurantservice.application.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for restaurant information.
 */
public class RestaurantDto {
    
    private String id;
    private String restaurantId;
    private String name;
    private String cuisine;
    private AddressDto address;
    private List<MenuItemDto> menu;
    private boolean isActive;
    private int version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public RestaurantDto() {}

    public RestaurantDto(String id, String restaurantId, String name, String cuisine, 
                        AddressDto address, List<MenuItemDto> menu, boolean isActive, 
                        int version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.cuisine = cuisine;
        this.address = address;
        this.menu = menu;
        this.isActive = isActive;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public AddressDto getAddress() {
        return address;
    }

    public void setAddress(AddressDto address) {
        this.address = address;
    }

    public List<MenuItemDto> getMenu() {
        return menu;
    }

    public void setMenu(List<MenuItemDto> menu) {
        this.menu = menu;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}