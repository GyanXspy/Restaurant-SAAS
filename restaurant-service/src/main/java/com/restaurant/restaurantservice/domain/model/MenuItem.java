package com.restaurant.restaurantservice.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Value object representing a menu item.
 * Encapsulates menu item validation and business rules.
 */
public class MenuItem {
    
    private final String itemId;
    private final String name;
    private final String description;
    private final BigDecimal price;
    private final String category;
    private final boolean available;

    public MenuItem(String name, String description, BigDecimal price, String category, boolean available) {
        this(UUID.randomUUID().toString(), name, description, price, category, available);
    }

    public MenuItem(String itemId, String name, String description, BigDecimal price, String category, boolean available) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("Item ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Menu item name cannot be null or empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Menu item price cannot be null or negative");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Menu item category cannot be null or empty");
        }
        
        this.itemId = itemId.trim();
        this.name = name.trim();
        this.description = description != null ? description.trim() : "";
        this.price = price;
        this.category = category.trim();
        this.available = available;
    }

    public MenuItem withAvailability(boolean available) {
        return new MenuItem(this.itemId, this.name, this.description, this.price, this.category, available);
    }

    public MenuItem withPrice(BigDecimal newPrice) {
        return new MenuItem(this.itemId, this.name, this.description, newPrice, this.category, this.available);
    }

    public MenuItem withDescription(String newDescription) {
        return new MenuItem(this.itemId, this.name, newDescription, this.price, this.category, this.available);
    }

    public String getItemId() {
        return itemId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public boolean isAvailable() {
        return available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return Objects.equals(itemId, menuItem.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }

    @Override
    public String toString() {
        return String.format("MenuItem{id='%s', name='%s', price=%s, category='%s', available=%s}",
                itemId, name, price, category, available);
    }
}