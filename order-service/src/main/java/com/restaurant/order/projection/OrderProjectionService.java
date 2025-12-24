package com.restaurant.order.projection;

import com.restaurant.order.model.Order;
import com.restaurant.order.readmodel.OrderReadModel;
import com.restaurant.order.readmodel.OrderReadModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

/**
 * Service to project write model changes to read model.
 * Syncs MySQL write database to MongoDB read database.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProjectionService {
    
    private final OrderReadModelRepository readModelRepository;
    
    public void projectOrder(Order order) {
        log.debug("Projecting order to MongoDB read model: {}", order.getId());
        
        OrderReadModel readModel = new OrderReadModel();
        readModel.setId(order.getId());
        readModel.setCustomerId(order.getCustomerId());
        readModel.setRestaurantId(order.getRestaurantId());
        readModel.setTotalAmount(order.getTotalAmount());
        readModel.setStatus(order.getStatus());
        readModel.setPaymentId(order.getPaymentId());
        readModel.setCreatedAt(order.getCreatedAt());
        readModel.setUpdatedAt(order.getUpdatedAt());
        readModel.setItemCount(order.getItems().size());
        
        // Create denormalized items summary
        String itemsSummary = order.getItems().stream()
            .map(item -> String.format("%s x%d", item.getName(), item.getQuantity()))
            .collect(Collectors.joining(", "));
        readModel.setItemsSummary(itemsSummary);
        
        readModelRepository.save(readModel);
        log.debug("Order projected to MongoDB read model: {}", order.getId());
    }
    
    public void deleteOrder(String orderId) {
        log.debug("Deleting order from MongoDB read model: {}", orderId);
        readModelRepository.deleteById(orderId);
    }
}
