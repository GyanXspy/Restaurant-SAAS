package com.restaurant.cart.service;

import com.restaurant.cart.domain.Cart;
import com.restaurant.cart.domain.CartItem;
import com.restaurant.cart.domain.CartStatus;
import com.restaurant.cart.repository.CartRepository;
import com.restaurant.events.publisher.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private CartService cartService;

    private String customerId;
    private String cartId;
    private String restaurantId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID().toString();
        cartId = UUID.randomUUID().toString();
        restaurantId = UUID.randomUUID().toString();
    }

    @Test
    void getOrCreateCart_ExistingActiveCart_ShouldReturnExistingCart() {
        // Arrange
        Cart existingCart = new Cart(cartId, customerId);
        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE))
            .thenReturn(Optional.of(existingCart));

        // Act
        Cart result = cartService.getOrCreateCart(customerId);

        // Assert
        assertEquals(existingCart, result);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void getOrCreateCart_NoExistingCart_ShouldCreateNewCart() {
        // Arrange
        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cart result = cartService.getOrCreateCart(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(customerId, result.getCustomerId());
        assertEquals(CartStatus.ACTIVE, result.getStatus());
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void addItemToCart_ValidItem_ShouldAddItemAndPublishEvent() {
        // Arrange
        Cart cart = new Cart(cartId, customerId);
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        
        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cart result = cartService.addItemToCart(customerId, item);

        // Assert
        assertEquals(1, result.getItems().size());
        assertEquals(item.getItemId(), result.getItems().get(0).getItemId());
        assertEquals(restaurantId, result.getRestaurantId());
        verify(cartRepository).save(cart);
        verify(eventPublisher).publish(any());
    }

    @Test
    void removeItemFromCart_ExistingItem_ShouldRemoveItemAndPublishEvent() {
        // Arrange
        Cart cart = new Cart(cartId, customerId);
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        cart.addItem(item);
        
        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Cart result = cartService.removeItemFromCart(customerId, "item1");

        // Assert
        assertTrue(result.getItems().isEmpty());
        verify(cartRepository).save(cart);
        verify(eventPublisher).publish(any());
    }

    @Test
    void validateCart_ValidCart_ShouldReturnEmptyErrorList() {
        // Arrange
        Cart cart = new Cart(cartId, customerId);
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        cart.addItem(item);
        
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        // Act
        List<String> errors = cartService.validateCart(cartId);

        // Assert
        assertTrue(errors.isEmpty());
    }

    @Test
    void validateCart_EmptyCart_ShouldReturnValidationErrors() {
        // Arrange
        Cart emptyCart = new Cart(cartId, customerId);
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(emptyCart));

        // Act
        List<String> errors = cartService.validateCart(cartId);

        // Assert
        assertFalse(errors.isEmpty());
        assertTrue(errors.contains("Cart is empty"));
        assertTrue(errors.contains("No restaurant selected"));
    }

    @Test
    void validateCart_CartNotFound_ShouldReturnNotFoundError() {
        // Arrange
        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

        // Act
        List<String> errors = cartService.validateCart(cartId);

        // Assert
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Cart not found"));
    }

    @Test
    void markCartAsCheckedOut_ExistingCart_ShouldUpdateStatus() {
        // Arrange
        Cart cart = new Cart(cartId, customerId);
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        cartService.markCartAsCheckedOut(cartId);

        // Assert
        assertEquals(CartStatus.CHECKED_OUT, cart.getStatus());
        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_ExistingCart_ShouldClearItemsAndPublishEvent() {
        // Arrange
        Cart cart = new Cart(cartId, customerId);
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, restaurantId);
        cart.addItem(item);
        
        when(cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE))
            .thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        cartService.clearCart(customerId);

        // Assert
        assertTrue(cart.getItems().isEmpty());
        assertNull(cart.getRestaurantId());
        verify(cartRepository).save(cart);
        verify(eventPublisher).publish(any());
    }
}