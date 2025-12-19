package com.restaurant.cart.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    private String cartId;
    private String customerId;
    private String restaurantId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        cartId = UUID.randomUUID().toString();
        customerId = UUID.randomUUID().toString();
        restaurantId = UUID.randomUUID().toString();
        cart = new Cart(cartId, customerId);
    }

    @Test
    void constructor_ValidParameters_ShouldCreateCart() {
        // Assert
        assertEquals(cartId, cart.getCartId());
        assertEquals(customerId, cart.getCustomerId());
        assertEquals(CartStatus.ACTIVE, cart.getStatus());
        assertTrue(cart.getItems().isEmpty());
        assertNotNull(cart.getCreatedAt());
        assertNotNull(cart.getUpdatedAt());
        assertNotNull(cart.getExpiresAt());
    }

    @Test
    void constructor_NullCartId_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new Cart(null, customerId));
    }

    @Test
    void constructor_NullCustomerId_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new Cart(cartId, null));
    }

    @Test
    void addItem_ValidItem_ShouldAddItem() {
        // Arrange
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);

        // Act
        cart.addItem(item);

        // Assert
        assertEquals(1, cart.getItems().size());
        assertEquals(item.getItemId(), cart.getItems().get(0).getItemId());
        assertEquals(restaurantId, cart.getRestaurantId());
        assertEquals(BigDecimal.valueOf(21.98), cart.getTotalAmount());
    }

    @Test
    void addItem_SameItemTwice_ShouldUpdateQuantity() {
        // Arrange
        CartItem item1 = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        CartItem item2 = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 1, restaurantId);

        // Act
        cart.addItem(item1);
        cart.addItem(item2);

        // Assert
        assertEquals(1, cart.getItems().size());
        assertEquals(3, cart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(32.97), cart.getTotalAmount());
    }

    @Test
    void addItem_DifferentRestaurant_ShouldThrowException() {
        // Arrange
        CartItem item1 = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        CartItem item2 = new CartItem("item2", "Another Item", BigDecimal.valueOf(5.99), 1, "different-restaurant");

        // Act
        cart.addItem(item1);

        // Assert
        assertThrows(IllegalArgumentException.class, () -> cart.addItem(item2));
    }

    @Test
    void removeItem_ExistingItem_ShouldRemoveItem() {
        // Arrange
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        cart.addItem(item);

        // Act
        cart.removeItem("item1");

        // Assert
        assertTrue(cart.getItems().isEmpty());
        assertNull(cart.getRestaurantId());
        assertEquals(BigDecimal.ZERO, cart.getTotalAmount());
    }

    @Test
    void updateItemQuantity_ExistingItem_ShouldUpdateQuantity() {
        // Arrange
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        cart.addItem(item);

        // Act
        cart.updateItemQuantity("item1", 5);

        // Assert
        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        assertEquals(BigDecimal.valueOf(54.95), cart.getTotalAmount());
    }

    @Test
    void updateItemQuantity_ZeroQuantity_ShouldRemoveItem() {
        // Arrange
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        cart.addItem(item);

        // Act
        cart.updateItemQuantity("item1", 0);

        // Assert
        assertTrue(cart.getItems().isEmpty());
    }

    @Test
    void updateItemQuantity_NonExistentItem_ShouldThrowException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> cart.updateItemQuantity("non-existent", 5));
    }

    @Test
    void clearCart_WithItems_ShouldClearAllItems() {
        // Arrange
        CartItem item1 = new CartItem("item1", "Test Item 1", BigDecimal.valueOf(10.99), 2, restaurantId);
        CartItem item2 = new CartItem("item2", "Test Item 2", BigDecimal.valueOf(5.99), 1, restaurantId);
        cart.addItem(item1);
        cart.addItem(item2);

        // Act
        cart.clearCart();

        // Assert
        assertTrue(cart.getItems().isEmpty());
        assertNull(cart.getRestaurantId());
        assertEquals(BigDecimal.ZERO, cart.getTotalAmount());
    }

    @Test
    void validateForCheckout_ValidCart_ShouldReturnNoErrors() {
        // Arrange
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        cart.addItem(item);

        // Act
        List<String> errors = cart.validateForCheckout();

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateForCheckout_EmptyCart_ShouldReturnErrors() {
        // Act
        List<String> errors = cart.validateForCheckout();

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Cart is empty"));
        assertTrue(errors.contains("No restaurant selected"));
    }

    @Test
    void validateForCheckout_ExpiredCart_ShouldReturnErrors() {
        // Arrange
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        cart.addItem(item);
        cart.markAsExpired();

        // Act
        List<String> errors = cart.validateForCheckout();

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Cart is not active"));
    }

    @Test
    void getTotalItemCount_MultipleItems_ShouldReturnCorrectCount() {
        // Arrange
        CartItem item1 = new CartItem("item1", "Test Item 1", BigDecimal.valueOf(10.99), 2, restaurantId);
        CartItem item2 = new CartItem("item2", "Test Item 2", BigDecimal.valueOf(5.99), 3, restaurantId);
        cart.addItem(item1);
        cart.addItem(item2);

        // Act
        int totalCount = cart.getTotalItemCount();

        // Assert
        assertEquals(5, totalCount);
    }

    @Test
    void markAsExpired_ActiveCart_ShouldChangeStatus() {
        // Act
        cart.markAsExpired();

        // Assert
        assertEquals(CartStatus.EXPIRED, cart.getStatus());
    }

    @Test
    void markAsCheckedOut_ActiveCart_ShouldChangeStatus() {
        // Act
        cart.markAsCheckedOut();

        // Assert
        assertEquals(CartStatus.CHECKED_OUT, cart.getStatus());
    }

    @Test
    void addItem_ExpiredCart_ShouldThrowException() {
        // Arrange
        cart.markAsExpired();
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> cart.addItem(item));
    }
}