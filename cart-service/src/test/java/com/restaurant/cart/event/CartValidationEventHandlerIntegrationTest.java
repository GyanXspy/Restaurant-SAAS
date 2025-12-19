package com.restaurant.cart.event;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.test.context.ActiveProfiles;

import com.restaurant.cart.domain.Cart;
import com.restaurant.cart.domain.CartItem;
import com.restaurant.cart.repository.CartRepository;
import com.restaurant.events.CartValidationCompletedEvent;
import com.restaurant.events.CartValidationRequestedEvent;
import com.restaurant.events.publisher.EventPublisher;

@SpringBootTest
@ActiveProfiles("test")
class CartValidationEventHandlerIntegrationTest {

    @Autowired
    private CartValidationEventHandler eventHandler;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private EventPublisher eventPublisher;

    @MockBean
    private Acknowledgment acknowledgment;

    private String cartId;
    private String customerId;
    private String orderId;
    private String sagaId;

    @BeforeEach
    void setUp() {
        cartId = UUID.randomUUID().toString();
        customerId = UUID.randomUUID().toString();
        orderId = UUID.randomUUID().toString();
        sagaId = UUID.randomUUID().toString();
    }

    @Test
    void handleCartValidationRequested_ValidCart_ShouldPublishSuccessEvent() {
        // Arrange
        Cart validCart = createValidCart();
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(validCart));
        when(cartRepository.save(any(Cart.class))).thenReturn(validCart);

        CartValidationRequestedEvent requestEvent = new CartValidationRequestedEvent(
            sagaId, cartId, customerId, orderId, 1
        );

        // Act
        eventHandler.handleCartValidationRequested(requestEvent, "cart-validation-requested", 0, 0L, acknowledgment);

        // Assert
        ArgumentCaptor<CartValidationCompletedEvent> eventCaptor = ArgumentCaptor.forClass(CartValidationCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        CartValidationCompletedEvent publishedEvent = eventCaptor.getValue();
        assertTrue(publishedEvent.isValid());
        assertTrue(publishedEvent.getValidationErrors().isEmpty());
        assertEquals(cartId, publishedEvent.getCartId());
        assertEquals(orderId, publishedEvent.getOrderId());
        
        verify(acknowledgment).acknowledge();
        verify(cartRepository).save(any(Cart.class)); // Cart should be marked as checked out
    }

    @Test
    void handleCartValidationRequested_EmptyCart_ShouldPublishFailureEvent() {
        // Arrange
        Cart emptyCart = new Cart(cartId, customerId);
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(emptyCart));

        CartValidationRequestedEvent requestEvent = new CartValidationRequestedEvent(
            sagaId, cartId, customerId, orderId, 1
        );

        // Act
        eventHandler.handleCartValidationRequested(requestEvent, "cart-validation-requested", 0, 0L, acknowledgment);

        // Assert
        ArgumentCaptor<CartValidationCompletedEvent> eventCaptor = ArgumentCaptor.forClass(CartValidationCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        CartValidationCompletedEvent publishedEvent = eventCaptor.getValue();
        assertFalse(publishedEvent.isValid());
        assertFalse(publishedEvent.getValidationErrors().isEmpty());
        assertTrue(publishedEvent.getValidationErrors().contains("Cart is empty"));
        
        verify(acknowledgment).acknowledge();
        verify(cartRepository, never()).save(any(Cart.class)); // Cart should not be modified
    }

    @Test
    void handleCartValidationRequested_ExpiredCart_ShouldPublishFailureEvent() {
        // Arrange
        Cart expiredCart = createValidCart();
        // Manually set expiration date to past
        expiredCart.markAsExpired();
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(expiredCart));

        CartValidationRequestedEvent requestEvent = new CartValidationRequestedEvent(
            sagaId, cartId, customerId, orderId, 1
        );

        // Act
        eventHandler.handleCartValidationRequested(requestEvent, "cart-validation-requested", 0, 0L, acknowledgment);

        // Assert
        ArgumentCaptor<CartValidationCompletedEvent> eventCaptor = ArgumentCaptor.forClass(CartValidationCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        CartValidationCompletedEvent publishedEvent = eventCaptor.getValue();
        assertFalse(publishedEvent.isValid());
        assertFalse(publishedEvent.getValidationErrors().isEmpty());
        assertTrue(publishedEvent.getValidationErrors().contains("Cart is not active"));
        
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleCartValidationRequested_CartNotFound_ShouldPublishFailureEvent() {
        // Arrange
        when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

        CartValidationRequestedEvent requestEvent = new CartValidationRequestedEvent(
            sagaId, cartId, customerId, orderId, 1
        );

        // Act
        eventHandler.handleCartValidationRequested(requestEvent, "cart-validation-requested", 0, 0L, acknowledgment);

        // Assert
        ArgumentCaptor<CartValidationCompletedEvent> eventCaptor = ArgumentCaptor.forClass(CartValidationCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        CartValidationCompletedEvent publishedEvent = eventCaptor.getValue();
        assertFalse(publishedEvent.isValid());
        assertFalse(publishedEvent.getValidationErrors().isEmpty());
        assertTrue(publishedEvent.getValidationErrors().get(0).contains("Cart not found"));
        
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleCartValidationRequested_DatabaseError_ShouldPublishFailureEventAndNotAcknowledge() {
        // Arrange
        when(cartRepository.findById(cartId)).thenThrow(new RuntimeException("Database connection error"));

        CartValidationRequestedEvent requestEvent = new CartValidationRequestedEvent(
            sagaId, cartId, customerId, orderId, 1
        );

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            eventHandler.handleCartValidationRequested(requestEvent, "cart-validation-requested", 0, 0L, acknowledgment);
        });

        // Verify failure event is published
        ArgumentCaptor<CartValidationCompletedEvent> eventCaptor = ArgumentCaptor.forClass(CartValidationCompletedEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        CartValidationCompletedEvent publishedEvent = eventCaptor.getValue();
        assertFalse(publishedEvent.isValid());
        assertTrue(publishedEvent.getValidationErrors().get(0).contains("Internal error during cart validation"));
        
        // Message should not be acknowledged for retry
        verify(acknowledgment, never()).acknowledge();
    }

    private Cart createValidCart() {
        Cart cart = new Cart(cartId, customerId);
        CartItem item = new CartItem("item1", "Test Item", BigDecimal.valueOf(10.99), 2, "restaurant1");
        cart.addItem(item);
        return cart;
    }
}