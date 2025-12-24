package com.restaurant.order.service;

import com.restaurant.order.dto.CreateOrderRequest;
import com.restaurant.order.dto.OrderItemDto;
import com.restaurant.order.dto.OrderResponse;
import com.restaurant.order.model.Order;
import com.restaurant.order.model.OrderItem;
import com.restaurant.order.model.OrderStatus;
import com.restaurant.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    
    private final OrderRepository orderRepository;
    
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());
        
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setRestaurantId(request.getRestaurantId());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus(OrderStatus.PENDING);
        
        List<OrderItem> items = request.getItems().stream()
            .map(dto -> new OrderItem(dto.getMenuItemId(), dto.getName(), dto.getPrice(), dto.getQuantity()))
            .collect(Collectors.toList());
        order.setItems(items);
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created with ID: {}", savedOrder.getId());
        
        return mapToResponse(savedOrder);
    }
    
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        return mapToResponse(order);
    }
    
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public OrderResponse confirmOrder(String orderId, String paymentId) {
        log.info("Confirming order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentId(paymentId);
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order confirmed: {}", orderId);
        
        return mapToResponse(savedOrder);
    }
    
    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        log.info("Cancelling order: {}", orderId);
        
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.setStatus(OrderStatus.CANCELLED);
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order cancelled: {}", orderId);
        
        return mapToResponse(savedOrder);
    }
    
    private OrderResponse mapToResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setRestaurantId(order.getRestaurantId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setPaymentId(order.getPaymentId());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        
        List<OrderItemDto> itemDtos = order.getItems().stream()
            .map(item -> {
                OrderItemDto dto = new OrderItemDto();
                dto.setMenuItemId(item.getMenuItemId());
                dto.setName(item.getName());
                dto.setPrice(item.getPrice());
                dto.setQuantity(item.getQuantity());
                return dto;
            })
            .collect(Collectors.toList());
        response.setItems(itemDtos);
        
        return response;
    }
}
