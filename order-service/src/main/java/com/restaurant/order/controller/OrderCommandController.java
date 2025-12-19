package com.restaurant.order.controller;

import com.restaurant.order.command.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for Order command operations (write side of CQRS).
 * Handles order creation, confirmation, and cancellation.
 */
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderCommandController {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderCommandController.class);
    
    private final OrderCommandHandler commandHandler;
    
    public OrderCommandController(OrderCommandHandler commandHandler) {
        this.commandHandler = commandHandler;
    }
    
    /**
     * Creates a new order.
     * 
     * @param command the create order command
     * @return ResponseEntity with the created order ID
     */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderCommand command) {
        logger.info("Received create order request for customer: {}, restaurant: {}", 
                   command.getCustomerId(), command.getRestaurantId());
        
        try {
            String orderId = commandHandler.handle(command);
            
            CreateOrderResponse response = new CreateOrderResponse(orderId, "Order created successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            logger.error("Failed to create order for customer: {}", command.getCustomerId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new CreateOrderResponse(null, "Failed to create order: " + e.getMessage()));
        }
    }
    
    /**
     * Confirms an order after successful payment.
     * 
     * @param orderId the order ID
     * @param command the confirm order command
     * @return ResponseEntity with confirmation status
     */
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<OrderCommandResponse> confirmOrder(
            @PathVariable String orderId, 
            @Valid @RequestBody ConfirmOrderCommand command) {
        
        logger.info("Received confirm order request for order: {}", orderId);
        
        // Ensure the path variable matches the command
        command.setOrderId(orderId);
        
        try {
            commandHandler.handle(command);
            
            OrderCommandResponse response = new OrderCommandResponse("Order confirmed successfully");
            return ResponseEntity.ok(response);
            
        } catch (OrderNotFoundException e) {
            logger.error("Order not found for confirmation: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new OrderCommandResponse("Order not found"));
        } catch (Exception e) {
            logger.error("Failed to confirm order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OrderCommandResponse("Failed to confirm order: " + e.getMessage()));
        }
    }
    
    /**
     * Cancels an order.
     * 
     * @param orderId the order ID
     * @param command the cancel order command
     * @return ResponseEntity with cancellation status
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<OrderCommandResponse> cancelOrder(
            @PathVariable String orderId, 
            @Valid @RequestBody CancelOrderCommand command) {
        
        logger.info("Received cancel order request for order: {}", orderId);
        
        // Ensure the path variable matches the command
        command.setOrderId(orderId);
        
        try {
            commandHandler.handle(command);
            
            OrderCommandResponse response = new OrderCommandResponse("Order cancelled successfully");
            return ResponseEntity.ok(response);
            
        } catch (OrderNotFoundException e) {
            logger.error("Order not found for cancellation: {}", orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new OrderCommandResponse("Order not found"));
        } catch (Exception e) {
            logger.error("Failed to cancel order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new OrderCommandResponse("Failed to cancel order: " + e.getMessage()));
        }
    }
}