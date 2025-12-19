package com.restaurant.order.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.order.domain.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Query handler for Order read operations.
 * Handles all read operations (queries) for the Order domain.
 * Follows CQRS pattern by serving data from optimized read models.
 */
@Service
@Transactional(readOnly = true)
public class OrderQueryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderQueryHandler.class);
    
    private final OrderReadModelRepository repository;
    private final ObjectMapper objectMapper;
    
    public OrderQueryHandler(OrderReadModelRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Handles order query by ID.
     * 
     * @param query the order query
     * @return Optional containing the OrderView if found
     */
    public Optional<OrderView> handle(OrderQuery query) {
        if (query.getOrderId() != null) {
            return findById(query.getOrderId());
        }
        
        throw new IllegalArgumentException("Order ID is required for single order query");
    }
    
    /**
     * Handles paginated order queries with filtering.
     * 
     * @param query the order query with filters and pagination
     * @return Page of OrderView objects
     */
    public Page<OrderView> handlePagedQuery(OrderQuery query) {
        logger.debug("Handling paged order query: customerId={}, restaurantId={}, status={}, page={}, size={}",
                    query.getCustomerId(), query.getRestaurantId(), query.getStatus(), 
                    query.getPage(), query.getSize());
        
        Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
        Page<OrderReadModel> readModels;
        
        // Apply filters based on query parameters
        if (query.getCustomerId() != null && query.getStatus() != null) {
            readModels = repository.findByCustomerIdAndStatusOrderByCreatedAtDesc(
                query.getCustomerId(), query.getStatus(), pageable);
        } else if (query.getRestaurantId() != null && query.getStatus() != null) {
            readModels = repository.findByRestaurantIdAndStatusOrderByCreatedAtDesc(
                query.getRestaurantId(), query.getStatus(), pageable);
        } else if (query.getCustomerId() != null) {
            readModels = repository.findByCustomerIdOrderByCreatedAtDesc(
                query.getCustomerId(), pageable);
        } else if (query.getRestaurantId() != null) {
            readModels = repository.findByRestaurantIdOrderByCreatedAtDesc(
                query.getRestaurantId(), pageable);
        } else if (query.getStatus() != null) {
            readModels = repository.findByStatusOrderByCreatedAtDesc(
                query.getStatus(), pageable);
        } else {
            readModels = repository.findAll(pageable);
        }
        
        return readModels.map(this::mapToOrderView);
    }
    
    /**
     * Gets recent orders for a customer.
     * 
     * @param customerId the customer ID
     * @return List of recent OrderView objects
     */
    public List<OrderView> getRecentOrdersForCustomer(String customerId) {
        logger.debug("Getting recent orders for customer: {}", customerId);
        
        List<OrderReadModel> readModels = repository.findTop10ByCustomerIdOrderByCreatedAtDesc(customerId);
        return readModels.stream()
            .map(this::mapToOrderView)
            .toList();
    }
    
    /**
     * Gets active orders for a restaurant.
     * 
     * @param restaurantId the restaurant ID
     * @return List of active OrderView objects
     */
    public List<OrderView> getActiveOrdersForRestaurant(String restaurantId) {
        logger.debug("Getting active orders for restaurant: {}", restaurantId);
        
        List<OrderReadModel> readModels = repository.findActiveOrdersByRestaurantId(restaurantId);
        return readModels.stream()
            .map(this::mapToOrderView)
            .toList();
    }
    
    /**
     * Gets order count by status for a restaurant.
     * 
     * @param restaurantId the restaurant ID
     * @param status the order status
     * @return count of orders
     */
    public long getOrderCountByStatus(String restaurantId, String status) {
        return repository.countByRestaurantIdAndStatus(restaurantId, status);
    }
    
    /**
     * Finds order by payment ID.
     * 
     * @param paymentId the payment ID
     * @return Optional containing the OrderView if found
     */
    public Optional<OrderView> findByPaymentId(String paymentId) {
        return repository.findByPaymentId(paymentId)
            .map(this::mapToOrderView);
    }
    
    private Optional<OrderView> findById(String orderId) {
        return repository.findById(orderId)
            .map(this::mapToOrderView);
    }
    
    /**
     * Maps OrderReadModel to OrderView.
     */
    private OrderView mapToOrderView(OrderReadModel readModel) {
        List<OrderItem> items = deserializeItems(readModel.getItems());
        
        return new OrderView(
            readModel.getId(),
            readModel.getCustomerId(),
            readModel.getRestaurantId(),
            readModel.getRestaurantName(),
            items,
            readModel.getStatus(),
            readModel.getTotalAmount(),
            readModel.getCreatedAt(),
            readModel.getUpdatedAt()
        );
    }
    
    /**
     * Deserializes JSON string to List of OrderItem objects.
     */
    private List<OrderItem> deserializeItems(String itemsJson) {
        if (itemsJson == null || itemsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            return objectMapper.readValue(itemsJson, new TypeReference<List<OrderItem>>() {});
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize order items: {}", itemsJson, e);
            return Collections.emptyList();
        }
    }
}