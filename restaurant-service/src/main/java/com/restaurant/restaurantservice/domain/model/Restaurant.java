package com.restaurant.restaurantservice.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.MenuUpdatedEvent;
import com.restaurant.events.RestaurantAvailabilityChangedEvent;
import com.restaurant.events.RestaurantCreatedEvent;

/**
 * Restaurant aggregate root.
 * Manages restaurant information, menu items, and business rules.
 * Publishes domain events for state changes.
 */
@Document(collection = "restaurants")
public class Restaurant {
    
    @Id
    private String id;
    
    private RestaurantId restaurantId;
    private String name;
    private String cuisine;
    private Address address;
    private List<MenuItem> menu;
    private boolean isActive;
    private int version;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Domain events to be published (not persisted to MongoDB)
    @Transient
    private transient List<DomainEvent> domainEvents = new ArrayList<>();

    // Default constructor for MongoDB
    protected Restaurant() {
        this.menu = new ArrayList<>();
        this.domainEvents = new ArrayList<>();
    }

    public Restaurant(String name, String cuisine, Address address) {
        this();
        this.restaurantId = RestaurantId.generate();
        this.name = validateName(name);
        this.cuisine = validateCuisine(cuisine);
        this.address = validateAddress(address);
        this.isActive = true;
        this.version = 1;
        
        // Publish restaurant created event
        addDomainEvent(new RestaurantCreatedEvent(
            this.restaurantId.getValue(),
            this.name,
            this.cuisine,
            this.address.getFormattedAddress(),
            this.version
        ));
    }

    // Business methods
    public void addMenuItem(MenuItem menuItem) {
        if (menuItem == null) {
            throw new IllegalArgumentException("Menu item cannot be null");
        }
        
        // Check if item already exists
        if (menu.stream().anyMatch(item -> item.getItemId().equals(menuItem.getItemId()))) {
            throw new IllegalStateException("Menu item with ID " + menuItem.getItemId() + " already exists");
        }
        
        menu.add(menuItem);
        incrementVersion();
        
        // Publish menu updated event
        addDomainEvent(new MenuUpdatedEvent(
            this.restaurantId.getValue(),
            this.restaurantId.getValue(),
            List.of(menuItem.getItemId()),
            "ADDED",
            this.version
        ));
    }

    public void removeMenuItem(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        
        boolean removed = menu.removeIf(item -> item.getItemId().equals(itemId));
        if (!removed) {
            throw new IllegalStateException("Menu item with ID " + itemId + " not found");
        }
        
        incrementVersion();
        
        // Publish menu updated event
        addDomainEvent(new MenuUpdatedEvent(
            this.restaurantId.getValue(),
            this.restaurantId.getValue(),
            List.of(itemId),
            "REMOVED",
            this.version
        ));
    }

    public void updateMenuItem(MenuItem updatedItem) {
        if (updatedItem == null) {
            throw new IllegalArgumentException("Updated menu item cannot be null");
        }
        
        for (int i = 0; i < menu.size(); i++) {
            if (menu.get(i).getItemId().equals(updatedItem.getItemId())) {
                menu.set(i, updatedItem);
                incrementVersion();
                
                // Publish menu updated event
                addDomainEvent(new MenuUpdatedEvent(
                    this.restaurantId.getValue(),
                    this.restaurantId.getValue(),
                    List.of(updatedItem.getItemId()),
                    "MODIFIED",
                    this.version
                ));
                return;
            }
        }
        
        throw new IllegalStateException("Menu item with ID " + updatedItem.getItemId() + " not found");
    }

    public void updateItemAvailability(String itemId, boolean available) {
        Optional<MenuItem> itemOpt = menu.stream()
            .filter(item -> item.getItemId().equals(itemId))
            .findFirst();
            
        if (itemOpt.isEmpty()) {
            throw new IllegalStateException("Menu item with ID " + itemId + " not found");
        }
        
        MenuItem currentItem = itemOpt.get();
        if (currentItem.isAvailable() != available) {
            MenuItem updatedItem = currentItem.withAvailability(available);
            updateMenuItem(updatedItem);
        }
    }

    public void updateItemPrice(String itemId, BigDecimal newPrice) {
        Optional<MenuItem> itemOpt = menu.stream()
            .filter(item -> item.getItemId().equals(itemId))
            .findFirst();
            
        if (itemOpt.isEmpty()) {
            throw new IllegalStateException("Menu item with ID " + itemId + " not found");
        }
        
        MenuItem currentItem = itemOpt.get();
        MenuItem updatedItem = currentItem.withPrice(newPrice);
        updateMenuItem(updatedItem);
    }

    public void activate() {
        if (isActive) {
            throw new IllegalStateException("Restaurant is already active");
        }
        this.isActive = true;
        incrementVersion();
        
        // Publish availability changed event
        addDomainEvent(new RestaurantAvailabilityChangedEvent(
            this.restaurantId.getValue(),
            true,
            "Restaurant activated",
            this.version
        ));
    }

    public void deactivate() {
        if (!isActive) {
            throw new IllegalStateException("Restaurant is already inactive");
        }
        this.isActive = false;
        incrementVersion();
        
        // Publish availability changed event
        addDomainEvent(new RestaurantAvailabilityChangedEvent(
            this.restaurantId.getValue(),
            false,
            "Restaurant deactivated",
            this.version
        ));
    }

    // Query methods
    public Optional<MenuItem> findMenuItem(String itemId) {
        return menu.stream()
            .filter(item -> item.getItemId().equals(itemId))
            .findFirst();
    }

    public List<MenuItem> getAvailableMenuItems() {
        return menu.stream()
            .filter(MenuItem::isAvailable)
            .toList();
    }

    public List<MenuItem> getMenuItemsByCategory(String category) {
        return menu.stream()
            .filter(item -> item.getCategory().equalsIgnoreCase(category))
            .toList();
    }

    // Domain event management
    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    private void addDomainEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    // Validation methods
    private String validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant name cannot be null or empty");
        }
        return name.trim();
    }

    private String validateCuisine(String cuisine) {
        if (cuisine == null || cuisine.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant cuisine cannot be null or empty");
        }
        return cuisine.trim();
    }

    private Address validateAddress(Address address) {
        if (address == null) {
            throw new IllegalArgumentException("Restaurant address cannot be null");
        }
        return address;
    }

    private void incrementVersion() {
        this.version++;
    }

    // Getters
    public String getId() {
        return id;
    }

    public RestaurantId getRestaurantId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public String getCuisine() {
        return cuisine;
    }

    public Address getAddress() {
        return address;
    }

    public List<MenuItem> getMenu() {
        return Collections.unmodifiableList(menu);
    }

    public boolean isActive() {
        return isActive;
    }

    public int getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}