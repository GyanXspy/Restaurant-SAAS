package com.restaurant.cart.service;

import com.restaurant.cart.domain.Cart;
import com.restaurant.cart.domain.CartItem;
import com.restaurant.cart.domain.CartStatus;
import com.restaurant.cart.domain.events.CartClearedEvent;
import com.restaurant.cart.domain.events.CartItemAddedEvent;
import com.restaurant.cart.domain.events.CartItemRemovedEvent;
import com.restaurant.cart.repository.CartRepository;
import com.restaurant.events.publisher.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class implementing cart business logic and operations.
 */
@Service
public class CartService {

    private static final Logger logger = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final EventPublisher eventPublisher;

    @Autowired
    public CartService(CartRepository cartRepository, EventPublisher eventPublisher) {
        this.cartRepository = cartRepository;
        this.eventPublisher = eventPublisher;
    }

    public Cart getOrCreateCart(String customerId) {
        Optional<Cart> existingCart = cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE);
        
        if (existingCart.isPresent()) {
            Cart cart = existingCart.get();
            if (cart.isExpired()) {
                cart.markAsExpired();
                cartRepository.save(cart);
                return createNewCart(customerId);
            }
            return cart;
        }
        
        return createNewCart(customerId);
    }

    public Cart addItemToCart(String customerId, CartItem item) {
        Cart cart = getOrCreateCart(customerId);
        
        cart.addItem(item);
        Cart savedCart = cartRepository.save(cart);
        
        // Publish domain event
        CartItemAddedEvent event = new CartItemAddedEvent(
            cart.getCartId(),
            cart.getCustomerId(),
            cart.getRestaurantId(),
            item.getItemId(),
            item.getName(),
            item.getPrice(),
            item.getQuantity(),
            cart.getTotalAmount(),
            1
        );
        
        eventPublisher.publish(event);
        logger.info("Added item {} to cart {} for customer {}", item.getItemId(), cart.getCartId(), customerId);
        
        return savedCart;
    }

    public Cart removeItemFromCart(String customerId, String itemId) {
        Cart cart = getActiveCart(customerId);
        
        cart.removeItem(itemId);
        Cart savedCart = cartRepository.save(cart);
        
        // Publish domain event
        CartItemRemovedEvent event = new CartItemRemovedEvent(
            cart.getCartId(),
            cart.getCustomerId(),
            itemId,
            cart.getTotalAmount(),
            1
        );
        
        eventPublisher.publish(event);
        logger.info("Removed item {} from cart {} for customer {}", itemId, cart.getCartId(), customerId);
        
        return savedCart;
    }

    public Cart updateItemQuantity(String customerId, String itemId, int newQuantity) {
        Cart cart = getActiveCart(customerId);
        
        cart.updateItemQuantity(itemId, newQuantity);
        Cart savedCart = cartRepository.save(cart);
        
        logger.info("Updated item {} quantity to {} in cart {} for customer {}", 
                   itemId, newQuantity, cart.getCartId(), customerId);
        
        return savedCart;
    }

    public void clearCart(String customerId) {
        Cart cart = getActiveCart(customerId);
        
        cart.clearCart();
        cartRepository.save(cart);
        
        // Publish domain event
        CartClearedEvent event = new CartClearedEvent(
            cart.getCartId(),
            cart.getCustomerId(),
            1
        );
        
        eventPublisher.publish(event);
        logger.info("Cleared cart {} for customer {}", cart.getCartId(), customerId);
    }

    public Optional<Cart> getActiveCart(String customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)
            .filter(cart -> !cart.isExpired());
    }

    public Cart getActiveCart(String customerId) {
        return getActiveCart(customerId)
            .orElseThrow(() -> new IllegalArgumentException("No active cart found for customer: " + customerId));
    }

    public Optional<Cart> getCartById(String cartId) {
        return cartRepository.findById(cartId);
    }

    public List<String> validateCart(String cartId) {
        Optional<Cart> cartOpt = cartRepository.findById(cartId);
        
        if (cartOpt.isEmpty()) {
            return List.of("Cart not found: " + cartId);
        }
        
        Cart cart = cartOpt.get();
        return cart.validateForCheckout();
    }

    public void markCartAsCheckedOut(String cartId) {
        Optional<Cart> cartOpt = cartRepository.findById(cartId);
        
        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();
            cart.markAsCheckedOut();
            cartRepository.save(cart);
            logger.info("Marked cart {} as checked out", cartId);
        }
    }

    public void cleanupExpiredCarts() {
        List<Cart> expiredCarts = cartRepository.findExpiredCarts(LocalDateTime.now());
        
        for (Cart cart : expiredCarts) {
            cart.markAsExpired();
            cartRepository.save(cart);
        }
        
        logger.info("Marked {} expired carts", expiredCarts.size());
    }

    private Cart createNewCart(String customerId) {
        String cartId = UUID.randomUUID().toString();
        Cart cart = new Cart(cartId, customerId);
        Cart savedCart = cartRepository.save(cart);
        
        logger.info("Created new cart {} for customer {}", cartId, customerId);
        return savedCart;
    }
}