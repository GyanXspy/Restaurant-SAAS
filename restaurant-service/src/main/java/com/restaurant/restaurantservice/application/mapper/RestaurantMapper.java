package com.restaurant.restaurantservice.application.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.restaurant.restaurantservice.application.dto.AddressDto;
import com.restaurant.restaurantservice.application.dto.CreateRestaurantRequest;
import com.restaurant.restaurantservice.application.dto.MenuItemDto;
import com.restaurant.restaurantservice.application.dto.RestaurantDto;
import com.restaurant.restaurantservice.domain.model.Address;
import com.restaurant.restaurantservice.domain.model.MenuItem;
import com.restaurant.restaurantservice.domain.model.Restaurant;

/**
 * Mapper for converting between domain objects and DTOs.
 */
@Component
public class RestaurantMapper {

    /**
     * Converts CreateRestaurantRequest to Restaurant domain object.
     */
    public Restaurant toRestaurant(CreateRestaurantRequest request) {
        Address address = toAddress(request.getAddress());
        return new Restaurant(request.getName(), request.getCuisine(), address);
    }

    /**
     * Converts Restaurant domain object to RestaurantDto.
     */
    public RestaurantDto toRestaurantDto(Restaurant restaurant) {
        AddressDto addressDto = toAddressDto(restaurant.getAddress());
        List<MenuItemDto> menuDtos = restaurant.getMenu().stream()
            .map(this::toMenuItemDto)
            .collect(Collectors.toList());

        return new RestaurantDto(
            restaurant.getId(),
            restaurant.getRestaurantId().getValue(),
            restaurant.getName(),
            restaurant.getCuisine(),
            addressDto,
            menuDtos,
            restaurant.isActive(),
            restaurant.getVersion(),
            restaurant.getCreatedAt(),
            restaurant.getUpdatedAt()
        );
    }

    /**
     * Converts MenuItemDto to MenuItem domain object.
     */
    public MenuItem toMenuItem(MenuItemDto dto) {
        if (dto.getItemId() != null) {
            return new MenuItem(
                dto.getItemId(),
                dto.getName(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getCategory(),
                dto.isAvailable()
            );
        } else {
            return new MenuItem(
                dto.getName(),
                dto.getDescription(),
                dto.getPrice(),
                dto.getCategory(),
                dto.isAvailable()
            );
        }
    }

    /**
     * Converts MenuItem domain object to MenuItemDto.
     */
    public MenuItemDto toMenuItemDto(MenuItem menuItem) {
        return new MenuItemDto(
            menuItem.getItemId(),
            menuItem.getName(),
            menuItem.getDescription(),
            menuItem.getPrice(),
            menuItem.getCategory(),
            menuItem.isAvailable()
        );
    }

    /**
     * Converts AddressDto to Address domain object.
     */
    public Address toAddress(AddressDto dto) {
        return new Address(
            dto.getStreet(),
            dto.getCity(),
            dto.getZipCode(),
            dto.getCountry()
        );
    }

    /**
     * Converts Address domain object to AddressDto.
     */
    public AddressDto toAddressDto(Address address) {
        return new AddressDto(
            address.getStreet(),
            address.getCity(),
            address.getZipCode(),
            address.getCountry()
        );
    }

    /**
     * Converts list of restaurants to list of DTOs.
     */
    public List<RestaurantDto> toRestaurantDtos(List<Restaurant> restaurants) {
        return restaurants.stream()
            .map(this::toRestaurantDto)
            .collect(Collectors.toList());
    }
}