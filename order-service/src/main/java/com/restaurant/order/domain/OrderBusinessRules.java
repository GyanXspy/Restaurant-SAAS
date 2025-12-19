package com.restaurant.order.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Business rules and validation logic for Order aggregate.
 * Centralizes all business validation to ensure consistency.
 */
public class OrderBusinessRules {

    /**
     * Validates that an order can be created with the given parameters.
     * 
     * @param customerId the customer ID
     * @param restaurantId the restaurant ID
     * @param items the order items
     * @param totalAmount the total amount
     * @throws IllegalArgumentException if validation fails
     */
    public static void validateOrderCreation(String customerId, String restaurantId, 
                                           List<OrderItem> items, BigDecimal totalAmount) {
        validateCustomerId(customerId);
        validateRestaurantId(restaurantId);
        validateOrderItems(items);
        validateTotalAmount(totalAmount);
        validateCalculatedTotal(items, totalAmount);
    }

    /**
     * Validates that an order can be confirmed.
     * 
     * @param order the order to validate
     * @param paymentId the payment ID
     * @throws IllegalStateException if order cannot be confirmed
     * @throws IllegalArgumentException if payment ID is invalid
     */
    public static void validateOrderConfirmation(Order order, String paymentId) {
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order can only be confirmed when in PENDING status");
        }
        
        if (paymentId == null || paymentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Payment ID is required to confirm order");
        }
    }

    /**
     * Validates that an order can be cancelled.
     * 
     * @param order the order to validate
     * @throws IllegalStateException if order cannot be cancelled
     */
    public static void validateOrderCancellation(Order order) {
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Cannot cancel a confirmed order");
        }
        // Note: Already cancelled orders are allowed (no-op)
    }

    private static void validateCustomerId(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID is required");
        }
    }

    private static void validateRestaurantId(String restaurantId) {
        if (restaurantId == null || restaurantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant ID is required");
        }
    }

    private static void validateOrderItems(List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        
        // Validate each item
        for (OrderItem item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Order items cannot be null");
            }
        }
    }

    private static void validateTotalAmount(BigDecimal totalAmount) {
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Total amount must be greater than zero");
        }
    }

    private static void validateCalculatedTotal(List<OrderItem> items, BigDecimal providedTotal) {
        BigDecimal calculatedTotal = items.stream()
            .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
            
        if (calculatedTotal.compareTo(providedTotal) != 0) {
            throw new IllegalArgumentException("Provided total amount does not match calculated total");
        }
    }
}