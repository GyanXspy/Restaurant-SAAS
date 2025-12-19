package com.restaurant.order.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Manages retry logic for saga operations.
 * Implements exponential backoff and retry limits.
 */
@Service
public class SagaRetryManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaRetryManager.class);
    
    private final SagaTimeoutConfig timeoutConfig;
    private final SagaTimeoutManager timeoutManager;
    private final OrderSagaRepository sagaRepository;
    
    public SagaRetryManager(SagaTimeoutConfig timeoutConfig, 
                           SagaTimeoutManager timeoutManager,
                           OrderSagaRepository sagaRepository) {
        this.timeoutConfig = timeoutConfig;
        this.timeoutManager = timeoutManager;
        this.sagaRepository = sagaRepository;
    }
    
    /**
     * Attempts to retry a saga operation if retry limit hasn't been reached.
     * 
     * @param orderId The order ID
     * @param operation The operation to retry
     * @return true if retry was scheduled, false if retry limit exceeded
     */
    public boolean attemptRetry(String orderId, Runnable operation) {
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(orderId)
                .orElseThrow(() -> new SagaNotFoundException("Saga not found for order: " + orderId));
            
            if (sagaData.getRetryCount() >= timeoutConfig.getMaxRetryAttempts()) {
                logger.warn("Retry limit exceeded for order: {}. Current retry count: {}, Max attempts: {}", 
                           orderId, sagaData.getRetryCount(), timeoutConfig.getMaxRetryAttempts());
                return false;
            }
            
            // Increment retry count
            sagaData.incrementRetryCount();
            sagaRepository.save(sagaData);
            
            // Schedule retry with exponential backoff
            timeoutManager.scheduleRetry(orderId, sagaData.getRetryCount(), operation);
            
            logger.info("Scheduled retry {} for order: {}", sagaData.getRetryCount(), orderId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to schedule retry for order: {}", orderId, e);
            return false;
        }
    }
    
    /**
     * Resets the retry count for a saga (typically called on successful operation).
     */
    public void resetRetryCount(String orderId) {
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(orderId)
                .orElse(null);
            
            if (sagaData != null && sagaData.getRetryCount() > 0) {
                sagaData.setRetryCount(0);
                sagaRepository.save(sagaData);
                logger.debug("Reset retry count for order: {}", orderId);
            }
        } catch (Exception e) {
            logger.error("Failed to reset retry count for order: {}", orderId, e);
        }
    }
    
    /**
     * Checks if the saga has exceeded the retry limit.
     */
    public boolean hasExceededRetryLimit(String orderId) {
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(orderId)
                .orElse(null);
            
            if (sagaData == null) {
                return true; // Consider missing saga as exceeded limit
            }
            
            return sagaData.getRetryCount() >= timeoutConfig.getMaxRetryAttempts();
            
        } catch (Exception e) {
            logger.error("Failed to check retry limit for order: {}", orderId, e);
            return true; // Err on the side of caution
        }
    }
}