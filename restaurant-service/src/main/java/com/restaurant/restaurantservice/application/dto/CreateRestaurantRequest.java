package com.restaurant.restaurantservice.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new restaurant.
 */
public class CreateRestaurantRequest {
    
    @NotBlank(message = "Restaurant name is required")
    @Size(max = 100, message = "Restaurant name cannot exceed 100 characters")
    private String name;
    
    @NotBlank(message = "Cuisine type is required")
    @Size(max = 50, message = "Cuisine type cannot exceed 50 characters")
    private String cuisine;
    
    @NotNull(message = "Address is required")
    @Valid
    private AddressDto address;

    // Default constructor
    public CreateRestaurantRequest() {}

    public CreateRestaurantRequest(String name, String cuisine, AddressDto address) {
        this.name = name;
        this.cuisine = cuisine;
        this.address = address;
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
}