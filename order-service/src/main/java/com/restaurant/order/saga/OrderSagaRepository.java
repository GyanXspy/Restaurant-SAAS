package com.restaurant.order.saga;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing OrderSagaData persistence.
 * Provides methods for storing and retrieving saga state information.
 */
public interface OrderSagaRepository {
    
    /**
     * Saves or updates saga data.
     * 
     * @param sagaData the saga data to save
     * @return the saved saga data
     */
    OrderSagaData save(OrderSagaData sagaData);
    
    /**
     * Finds saga data by order ID.
     * 
     * @param orderId the order ID
     * @return optional containing saga data if found
     */
    Optional<OrderSagaData> findByOrderId(String orderId);
    
    /**
     * Finds all saga data by state.
     * 
     * @param state the saga state
     * @return list of saga data in the specified state
     */
    List<OrderSagaData> findByState(OrderSagaState state);
    
    /**
     * Finds all saga data that are in progress (not completed or failed).
     * 
     * @return list of in-progress saga data
     */
    List<OrderSagaData> findInProgressSagas();
    
    /**
     * Finds saga data that have exceeded timeout thresholds.
     * Used for timeout handling and cleanup.
     * 
     * @param timeoutMinutes the timeout threshold in minutes
     * @return list of timed-out saga data
     */
    List<OrderSagaData> findTimedOutSagas(int timeoutMinutes);
    
    /**
     * Deletes saga data by order ID.
     * 
     * @param orderId the order ID
     */
    void deleteByOrderId(String orderId);
}