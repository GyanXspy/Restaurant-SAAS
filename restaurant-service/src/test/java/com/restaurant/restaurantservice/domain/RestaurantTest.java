package com.restaurant.restaurantservice.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.MenuUpdatedEvent;
import com.restaurant.events.RestaurantCreatedEvent;
import com.restaurant.restaurantservice.domain.event.RestaurantAvailabilityChangedEvent;
import com.restaurant.restaurantservice.domain.model.Address;
import com.restaurant.restaurantservice.domain.model.MenuItem;
import com.restaurant.restaurantservice.domain.model.Restaurant;

/**
 * Unit tests for Restaurant domain model.
 */
class RestaurantTest {

    private Restaurant restaurant;
    private Address address;

    @BeforeEach
    void setUp() {
        address = new Address("123 Main St", "New York", "10001", "USA");
        restaurant = new Restaurant("Test Restaurant", "Italian", address);
    }

    @Test
    void shouldCreateRestaurantWithValidData() {
        // Then
        assertNotNull(restaurant.getRestaurantId());
        assertEquals("Test Restaurant", restaurant.getName());
        assertEquals("Italian", restaurant.getCuisine());
        assertEquals(address, restaurant.getAddress());
        assertTrue(restaurant.isActive());
        assertEquals(1, restaurant.getVersion());
        assertTrue(restaurant.getMenu().isEmpty());
        
        // Should have domain event
        List<DomainEvent> events = restaurant.getDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof RestaurantCreatedEvent);
    }

    @Test
    void shouldThrowExceptionForInvalidRestaurantData() {
        // When & Then
        assertThrows(IllegalArgumentException.class, 
            () -> new Restaurant(null, "Italian", address));
        assertThrows(IllegalArgumentException.class, 
            () -> new Restaurant("", "Italian", address));
        assertThrows(IllegalArgumentException.class, 
            () -> new Restaurant("Test", null, address));
        assertThrows(IllegalArgumentException.class, 
            () -> new Restaurant("Test", "", address));
        assertThrows(IllegalArgumentException.class, 
            () -> new Restaurant("Test", "Italian", null));
    }

    @Test
    void shouldAddMenuItemSuccessfully() {
        // Given
        MenuItem menuItem = new MenuItem("Pizza Margherita", "Classic pizza", 
            new BigDecimal("12.99"), "Pizza", true);

        // When
        restaurant.addMenuItem(menuItem);

        // Then
        assertEquals(1, restaurant.getMenu().size());
        assertTrue(restaurant.getMenu().contains(menuItem));
        assertEquals(2, restaurant.getVersion());
        
        // Should have menu updated event
        List<DomainEvent> events = restaurant.getDomainEvents();
        assertEquals(2, events.size()); // RestaurantCreated + MenuUpdated
        assertTrue(events.get(1) instanceof MenuUpdatedEvent);
        
        MenuUpdatedEvent menuEvent = (MenuUpdatedEvent) events.get(1);
        assertEquals("ADDED", menuEvent.getUpdateType());
        assertTrue(menuEvent.getUpdatedItemIds().contains(menuItem.getItemId()));
    }

    @Test
    void shouldThrowExceptionWhenAddingDuplicateMenuItem() {
        // Given
        MenuItem menuItem1 = new MenuItem("item-1", "Pizza", "Description", 
            new BigDecimal("12.99"), "Pizza", true);
        MenuItem menuItem2 = new MenuItem("item-1", "Another Pizza", "Description", 
            new BigDecimal("15.99"), "Pizza", true);

        restaurant.addMenuItem(menuItem1);

        // When & Then
        assertThrows(IllegalStateException.class, 
            () -> restaurant.addMenuItem(menuItem2));
    }

    @Test
    void shouldRemoveMenuItemSuccessfully() {
        // Given
        MenuItem menuItem = new MenuItem("Pizza Margherita", "Classic pizza", 
            new BigDecimal("12.99"), "Pizza", true);
        restaurant.addMenuItem(menuItem);
        restaurant.clearDomainEvents(); // Clear previous events

        // When
        restaurant.removeMenuItem(menuItem.getItemId());

        // Then
        assertTrue(restaurant.getMenu().isEmpty());
        assertEquals(3, restaurant.getVersion());
        
        // Should have menu updated event
        List<DomainEvent> events = restaurant.getDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof MenuUpdatedEvent);
        
        MenuUpdatedEvent menuEvent = (MenuUpdatedEvent) events.get(0);
        assertEquals("REMOVED", menuEvent.getUpdateType());
        assertTrue(menuEvent.getUpdatedItemIds().contains(menuItem.getItemId()));
    }

    @Test
    void shouldThrowExceptionWhenRemovingNonExistentMenuItem() {
        // When & Then
        assertThrows(IllegalStateException.class, 
            () -> restaurant.removeMenuItem("non-existent-id"));
    }

    @Test
    void shouldUpdateMenuItemSuccessfully() {
        // Given
        MenuItem originalItem = new MenuItem("Pizza Margherita", "Classic pizza", 
            new BigDecimal("12.99"), "Pizza", true);
        restaurant.addMenuItem(originalItem);
        
        MenuItem updatedItem = new MenuItem(originalItem.getItemId(), "Pizza Margherita", 
            "Updated description", new BigDecimal("14.99"), "Pizza", true);
        restaurant.clearDomainEvents(); // Clear previous events

        // When
        restaurant.updateMenuItem(updatedItem);

        // Then
        Optional<MenuItem> foundItem = restaurant.findMenuItem(originalItem.getItemId());
        assertTrue(foundItem.isPresent());
        assertEquals("Updated description", foundItem.get().getDescription());
        assertEquals(new BigDecimal("14.99"), foundItem.get().getPrice());
        assertEquals(3, restaurant.getVersion());
        
        // Should have menu updated event
        List<DomainEvent> events = restaurant.getDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof MenuUpdatedEvent);
        
        MenuUpdatedEvent menuEvent = (MenuUpdatedEvent) events.get(0);
        assertEquals("MODIFIED", menuEvent.getUpdateType());
    }

    @Test
    void shouldUpdateItemAvailability() {
        // Given
        MenuItem menuItem = new MenuItem("Pizza Margherita", "Classic pizza", 
            new BigDecimal("12.99"), "Pizza", true);
        restaurant.addMenuItem(menuItem);
        restaurant.clearDomainEvents();

        // When
        restaurant.updateItemAvailability(menuItem.getItemId(), false);

        // Then
        Optional<MenuItem> foundItem = restaurant.findMenuItem(menuItem.getItemId());
        assertTrue(foundItem.isPresent());
        assertFalse(foundItem.get().isAvailable());
    }

    @Test
    void shouldGetAvailableMenuItems() {
        // Given
        MenuItem availableItem = new MenuItem("Available Pizza", "Description", 
            new BigDecimal("12.99"), "Pizza", true);
        MenuItem unavailableItem = new MenuItem("Unavailable Pizza", "Description", 
            new BigDecimal("15.99"), "Pizza", false);
        
        restaurant.addMenuItem(availableItem);
        restaurant.addMenuItem(unavailableItem);

        // When
        List<MenuItem> availableItems = restaurant.getAvailableMenuItems();

        // Then
        assertEquals(1, availableItems.size());
        assertTrue(availableItems.contains(availableItem));
        assertFalse(availableItems.contains(unavailableItem));
    }

    @Test
    void shouldGetMenuItemsByCategory() {
        // Given
        MenuItem pizza = new MenuItem("Pizza", "Description", 
            new BigDecimal("12.99"), "Pizza", true);
        MenuItem pasta = new MenuItem("Pasta", "Description", 
            new BigDecimal("10.99"), "Pasta", true);
        MenuItem anotherPizza = new MenuItem("Another Pizza", "Description", 
            new BigDecimal("15.99"), "Pizza", true);
        
        restaurant.addMenuItem(pizza);
        restaurant.addMenuItem(pasta);
        restaurant.addMenuItem(anotherPizza);

        // When
        List<MenuItem> pizzaItems = restaurant.getMenuItemsByCategory("Pizza");

        // Then
        assertEquals(2, pizzaItems.size());
        assertTrue(pizzaItems.contains(pizza));
        assertTrue(pizzaItems.contains(anotherPizza));
        assertFalse(pizzaItems.contains(pasta));
    }

    @Test
    void shouldActivateAndDeactivateRestaurant() {
        // Given - restaurant is active by default
        assertTrue(restaurant.isActive());
        restaurant.clearDomainEvents();

        // When - deactivate
        restaurant.deactivate();

        // Then
        assertFalse(restaurant.isActive());
        assertEquals(2, restaurant.getVersion());
        
        List<DomainEvent> events = restaurant.getDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof RestaurantAvailabilityChangedEvent);
        
        RestaurantAvailabilityChangedEvent event = (RestaurantAvailabilityChangedEvent) events.get(0);
        assertFalse(event.isActive());
        assertEquals("Restaurant deactivated", event.getReason());
        
        restaurant.clearDomainEvents();

        // When - activate
        restaurant.activate();

        // Then
        assertTrue(restaurant.isActive());
        assertEquals(3, restaurant.getVersion());
        
        events = restaurant.getDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof RestaurantAvailabilityChangedEvent);
        
        event = (RestaurantAvailabilityChangedEvent) events.get(0);
        assertTrue(event.isActive());
        assertEquals("Restaurant activated", event.getReason());
    }

    @Test
    void shouldThrowExceptionWhenActivatingActiveRestaurant() {
        // Given - restaurant is active by default
        assertTrue(restaurant.isActive());

        // When & Then
        assertThrows(IllegalStateException.class, restaurant::activate);
    }

    @Test
    void shouldThrowExceptionWhenDeactivatingInactiveRestaurant() {
        // Given
        restaurant.deactivate();
        assertFalse(restaurant.isActive());

        // When & Then
        assertThrows(IllegalStateException.class, restaurant::deactivate);
    }
}