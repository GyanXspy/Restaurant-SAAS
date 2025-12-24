package com.restaurant.order.query;

import com.restaurant.order.dto.OrderResponse;
import com.restaurant.order.readmodel.OrderReadModel;
import com.restaurant.order.readmodel.OrderReadModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query Handler for Order read operations.
 * Reads from MongoDB read model following CQRS pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderQueryHandler {
    
    private final OrderReadModelRepository readModelRepository;
    
    public OrderResponse handle(OrderQuery query) {
        log.debug("Handling OrderQuery for order: {}", query.getOrderId());
        
        OrderReadModel readModel = readModelRepository.findById(query.getOrderId())
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + query.getOrderId()));
        
        return mapToResponse(readModel);
    }
    
    public List<OrderResponse> handle(CustomerOrdersQuery query) {
        log.debug("Handling CustomerOrdersQuery for customer: {}", query.getCustomerId());
        
        return readModelRepository.findByCustomerId(query.getCustomerId()).stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    private OrderResponse mapToResponse(OrderReadModel readModel) {
        OrderResponse response = new OrderResponse();
        response.setId(readModel.getId());
        response.setCustomerId(readModel.getCustomerId());
        response.setRestaurantId(readModel.getRestaurantId());
        response.setTotalAmount(readModel.getTotalAmount());
        response.setStatus(readModel.getStatus());
        response.setPaymentId(readModel.getPaymentId());
        response.setCreatedAt(readModel.getCreatedAt());
        response.setUpdatedAt(readModel.getUpdatedAt());
        
        // For read model, we use the denormalized summary
        // In a real system, you might store items separately or parse the summary
        response.setItems(new ArrayList<>()); // Simplified for now
        
        return response;
    }
}
