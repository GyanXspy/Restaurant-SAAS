package com.restaurant.order.controller;

import com.restaurant.order.query.OrderQuery;
import com.restaurant.order.query.OrderQueryHandler;
import com.restaurant.order.query.OrderView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST controller for Order query operations (read side of CQRS).
 * Handles order retrieval and search operations.
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderQueryController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderQueryController.class);
    
    private final OrderQueryHandler queryHandler;
    
    public OrderQueryController(OrderQueryHandler queryHandler) {
        this.queryHandler = queryHandler;
    }
    
    /**
     * Gets an order by ID.
     * 
     * @param orderId the order ID
     * @return ResponseEntity with the order view
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderView> getOrder(@PathVariable String orderId) {
        logger.info("Received get order request for order: {}", orderId);
        
        OrderQuery query = new OrderQuery(orderId);
        Optional<OrderView> orderView = queryHandler.handle(query);
        
        return orderView.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Searches orders with filtering and pagination.
     * 
     * @param customerId optional customer ID filter
     * @param restaurantId optional restaurant ID filter
     * @param status optional status filter
     * @param page page number (default: 0)
     * @param size page size (default: 20)
     * @return ResponseEntity with paginated order views
     */
    @GetMapping
    public ResponseEntity<Page<OrderView>> searchOrders(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String restaurantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        logger.info("Received search orders request: customerId={}, restaurantId={}, status={}, page={}, size={}",
                   customerId, restaurantId, status, page, size);
        
        OrderQuery query = new OrderQuery();
        query.setCustomerId(customerId);
        query.setRestaurantId(restaurantId);
        query.setStatus(status);
        query.setPage(page);
        query.setSize(size);
        
        Page<OrderView> orders = queryHandler.handlePagedQuery(query);
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Gets recent orders for a customer.
     * 
     * @param customerId the customer ID
     * @return ResponseEntity with recent order views
     */
    @GetMapping("/customers/{customerId}/recent")
    public ResponseEntity<List<OrderView>> getRecentOrdersForCustomer(@PathVariable String customerId) {
        logger.info("Received get recent orders request for customer: {}", customerId);
        
        List<OrderView> orders = queryHandler.getRecentOrdersForCustomer(customerId);
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Gets active orders for a restaurant.
     * 
     * @param restaurantId the restaurant ID
     * @return ResponseEntity with active order views
     */
    @GetMapping("/restaurants/{restaurantId}/active")
    public ResponseEntity<List<OrderView>> getActiveOrdersForRestaurant(@PathVariable String restaurantId) {
        logger.info("Received get active orders request for restaurant: {}", restaurantId);
        
        List<OrderView> orders = queryHandler.getActiveOrdersForRestaurant(restaurantId);
        return ResponseEntity.ok(orders);
    }
    
    /**
     * Gets order count by status for a restaurant.
     * 
     * @param restaurantId the restaurant ID
     * @param status the order status
     * @return ResponseEntity with order count
     */
    @GetMapping("/restaurants/{restaurantId}/count")
    public ResponseEntity<OrderCountResponse> getOrderCountByStatus(
            @PathVariable String restaurantId,
            @RequestParam String status) {
        
        logger.info("Received get order count request for restaurant: {}, status: {}", restaurantId, status);
        
        long count = queryHandler.getOrderCountByStatus(restaurantId, status);
        OrderCountResponse response = new OrderCountResponse(restaurantId, status, count);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Gets order by payment ID.
     * 
     * @param paymentId the payment ID
     * @return ResponseEntity with the order view
     */
    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<OrderView> getOrderByPaymentId(@PathVariable String paymentId) {
        logger.info("Received get order by payment ID request: {}", paymentId);
        
        Optional<OrderView> orderView = queryHandler.findByPaymentId(paymentId);
        
        return orderView.map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}