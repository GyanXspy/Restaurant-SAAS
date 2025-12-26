package com.restaurant.cart.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.restaurant.cart.domain.Cart;
import com.restaurant.cart.domain.CartItem;
import com.restaurant.cart.service.CartService;

/**
 * REST controller for cart operations.
 */
@RestController
@RequestMapping("/api/v1/carts")
public class CartController {

    private final CartService cartService;

    @Autowired
    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/{customerId}")
    public ResponseEntity<Cart> getCart(@PathVariable("customerId") String customerId) {
        Optional<Cart> cart = cartService.findActiveCart(customerId);
        return cart.map(ResponseEntity::ok)
                  .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{customerId}/items")
    public ResponseEntity<Cart> addItem(@PathVariable("customerId") String customerId, @RequestBody CartItem item) {
        try {
            Cart updatedCart = cartService.addItemToCart(customerId, item);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{customerId}/items/{itemId}")
    public ResponseEntity<Cart> removeItem(@PathVariable("customerId") String customerId, @PathVariable("itemId") String itemId) {
        try {
            Cart updatedCart = cartService.removeItemFromCart(customerId, itemId);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{customerId}/items/{itemId}")
    public ResponseEntity<Cart> updateItemQuantity(@PathVariable("customerId") String customerId, 
                                                  @PathVariable("itemId") String itemId, 
                                                  @RequestParam("quantity") int quantity) {
        try {
            Cart updatedCart = cartService.updateItemQuantity(customerId, itemId, quantity);
            return ResponseEntity.ok(updatedCart);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> clearCart(@PathVariable("customerId") String customerId) {
        try {
            cartService.clearCart(customerId);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}