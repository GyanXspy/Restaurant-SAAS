package com.restaurant.order.domain;

import com.restaurant.order.infrastructure.OrderEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Domain service for Order aggregate operations.
 * Encapsulates business logic and validation rules for order management.
 */
@Service
@Transactional
public class OrderService {
    
    private final OrderEventStore orderEventStore;
    
    public OrderService(OrderEventStore orderEventStore) {
        this.orderEventStore = orderEventStore;
    }
    
    /**
     * Creates a new order with validation.
     * 
     * @param customerId the ID of the customer placing the order
     * @param restaurantId the ID of the restaurant
     * @param items the list of items being ordered
     * @param totalAmount the total amount for the order
     * @return the created Order aggregate
     * @throws IllegalArgumentException if validation fails
     */
    public Order createOrder(String customerId, String restaurantId, 
                           List<OrderItem> items, BigDecimal totalAmount) {
        
        // Additional business validation can be added here
        validateOrderBusinessRules(customerId, restaurantId, items, totalAmount);
        
        Order order = Order.createOrder(customerId, restaurantId, items, totalAmount);
        orderEventStore.save(order);
        
        return order;
    }
    
    /**
     * Confirms an existing order.
     * 
     * @param orderId the ID of the order to confirm
     * @param paymentId the ID of the successful payment
     * @throws IllegalArgumentException if order not found
     * @throws IllegalStateException if order cannot be confirmed
     */
    public void confirmOrder(String orderId, String paymentId) {
        Order order = getOrderById(orderId);
        order.confirmOrder(paymentId);
        orderEventStore.save(order);
    }
    
    /**
     * Cancels an existing order.
     * 
     * @param orderId the ID of the order to cancel
     * @param reason the reason for cancellation
     * @throws IllegalArgumentException if order not found
     * @throws IllegalStateException if order cannot be cancelled
     */
    public void cancelOrder(String orderId, String reason) {
        Order order = getOrderById(orderId);
        order.cancelOrder(reason);
        orderEventStore.save(order);
    }
    
    /**
     * Retrieves an order by its ID.
     * 
     * @param orderId the ID of the order
     * @return the Order aggregate
     * @throws IllegalArgumentException if order not found
     */
    @Transactional(readOnly = true)
    public Order getOrderById(String orderId) {
        return orderEventStore.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    }
    
    /**
     * Checks if an order exists.
     * 
     * @param orderId the ID of the order
     * @return true if the order exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean orderExists(String orderId) {
        return orderEventStore.exists(orderId);
    }
    
    /**
     * Gets the current version of an order.
     * 
     * @param orderId the ID of the order
     * @return the current version
     */
    @Transactional(readOnly = true)
    public int getOrderVersion(String orderId) {
        return orderEventStore.getCurrentVersion(orderId);
    }
    
    /**
     * Validates business rules for order creation.
     * This method can be extended with additional business logic.
     */
    private void validateOrderBusinessRules(String customerId, String restaurantId, 
                                          List<OrderItem> items, BigDecimal totalAmount) {
        
        // Example business rules - can be extended based on requirements
        
        // Check minimum order amount
        BigDecimal minimumOrderAmount = new BigDecimal("5.00");
        if (totalAmount.compareTo(minimumOrderAmount) < 0) {
            throw new IllegalArgumentException(
                "Order total must be at least " + minimumOrderAmount);
        }
        
        // Check maximum order amount (to prevent fraud)
        BigDecimal maximumOrderAmount = new BigDecimal("1000.00");
        if (totalAmount.compareTo(maximumOrderAmount) > 0) {
            throw new IllegalArgumentException(
                "Order total cannot exceed " + maximumOrderAmount);
        }
        
        // Check maximum number of items
        int maxItems = 50;
        int totalQuantity = items.stream().mapToInt(OrderItem::getQuantity).sum();
        if (totalQuantity > maxItems) {
            throw new IllegalArgumentException(
                "Order cannot contain more than " + maxItems + " items");
        }
        
        // Validate that all items have valid prices
        for (OrderItem item : items) {
            if (item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                    "Item price must be greater than zero: " + item.getName());
            }
        }
    }
}