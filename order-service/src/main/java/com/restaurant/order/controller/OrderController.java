package com.restaurant.order.controller;

import com.restaurant.order.command.CancelOrderCommand;
import com.restaurant.order.command.ConfirmOrderCommand;
import com.restaurant.order.command.CreateOrderCommand;
import com.restaurant.order.command.OrderCommandHandler;
import com.restaurant.order.dto.CreateOrderRequest;
import com.restaurant.order.dto.OrderResponse;
import com.restaurant.order.query.CustomerOrdersQuery;
import com.restaurant.order.query.OrderQuery;
import com.restaurant.order.query.OrderQueryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Controller for Order operations using CQRS pattern.
 * Commands go to OrderCommandHandler, Queries go to OrderQueryHandler.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    
    private final OrderCommandHandler commandHandler;
    private final OrderQueryHandler queryHandler;
    
    @PostMapping
    public ResponseEntity<String> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Received create order request for customer: {}", request.getCustomerId());
        
        CreateOrderCommand command = new CreateOrderCommand(
            request.getCustomerId(),
            request.getRestaurantId(),
            request.getTotalAmount(),
            request.getItems().stream()
                .map(item -> new CreateOrderCommand.OrderItemCommand(
                    item.getMenuItemId(),
                    item.getName(),
                    item.getPrice(),
                    item.getQuantity()
                ))
                .collect(Collectors.toList())
        );
        
        String orderId = commandHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderId);
    }
    
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable("orderId") String orderId) {
        OrderQuery query = new OrderQuery(orderId);
        OrderResponse response = queryHandler.handle(query);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomer(@PathVariable("customerId") String customerId) {
        CustomerOrdersQuery query = new CustomerOrdersQuery(customerId);
        List<OrderResponse> orders = queryHandler.handle(query);
        return ResponseEntity.ok(orders);
    }
    
    @PutMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirmOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam("paymentId") String paymentId) {
        ConfirmOrderCommand command = new ConfirmOrderCommand(orderId, paymentId);
        commandHandler.handle(command);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam(value = "reason", required = false) String reason) {
        CancelOrderCommand command = new CancelOrderCommand(orderId, reason);
        commandHandler.handle(command);
        return ResponseEntity.ok().build();
    }
}
