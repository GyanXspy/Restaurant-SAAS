package com.restaurant.order.saga;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Manages timeouts for saga steps.
 * Schedules timeout tasks and handles timeout events.
 */
@Service
public class SagaTimeoutManager {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaTimeoutManager.class);
    
    private final TaskScheduler taskScheduler;
    private final SagaTimeoutConfig timeoutConfig;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> scheduledTimeouts = new ConcurrentHashMap<>();
    
    public SagaTimeoutManager(TaskScheduler taskScheduler, SagaTimeoutConfig timeoutConfig) {
        this.taskScheduler = taskScheduler;
        this.timeoutConfig = timeoutConfig;
    }
    
    /**
     * Schedules a timeout for cart validation step.
     */
    public void scheduleCartValidationTimeout(String orderId, SagaTimeoutHandler timeoutHandler) {
        scheduleTimeout(orderId, "CART_VALIDATION", timeoutConfig.getCartValidation(), timeoutHandler);
    }
    
    /**
     * Schedules a timeout for payment processing step.
     */
    public void schedulePaymentProcessingTimeout(String orderId, SagaTimeoutHandler timeoutHandler) {
        scheduleTimeout(orderId, "PAYMENT_PROCESSING", timeoutConfig.getPaymentProcessing(), timeoutHandler);
    }
    
    /**
     * Schedules a timeout for order confirmation step.
     */
    public void scheduleOrderConfirmationTimeout(String orderId, SagaTimeoutHandler timeoutHandler) {
        scheduleTimeout(orderId, "ORDER_CONFIRMATION", timeoutConfig.getOrderConfirmation(), timeoutHandler);
    }
    
    /**
     * Cancels any existing timeout for the given order.
     */
    public void cancelTimeout(String orderId) {
        String timeoutKey = createTimeoutKey(orderId);
        ScheduledFuture<?> scheduledFuture = scheduledTimeouts.remove(timeoutKey);
        
        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            scheduledFuture.cancel(false);
            logger.debug("Cancelled timeout for order: {}", orderId);
        }
    }
    
    /**
     * Schedules a retry with exponential backoff.
     */
    public void scheduleRetry(String orderId, int attemptNumber, Runnable retryAction) {
        Duration retryDelay = timeoutConfig.calculateRetryDelay(attemptNumber);
        
        logger.info("Scheduling retry {} for order: {} with delay: {}", 
                   attemptNumber, orderId, retryDelay);
        
        taskScheduler.schedule(() -> {
            try {
                retryAction.run();
            } catch (Exception e) {
                logger.error("Error during retry {} for order: {}", attemptNumber, orderId, e);
            }
        }, Instant.now().plus(retryDelay));
    }
    
    private void scheduleTimeout(String orderId, String stepName, Duration timeout, SagaTimeoutHandler timeoutHandler) {
        String timeoutKey = createTimeoutKey(orderId);
        
        // Cancel any existing timeout for this order
        cancelTimeout(orderId);
        
        logger.debug("Scheduling {} timeout for order: {} with duration: {}", stepName, orderId, timeout);
        
        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(() -> {
            try {
                logger.warn("Timeout occurred for order: {} in step: {}", orderId, stepName);
                timeoutHandler.handleTimeout(orderId, stepName);
            } catch (Exception e) {
                logger.error("Error handling timeout for order: {} in step: {}", orderId, stepName, e);
            } finally {
                scheduledTimeouts.remove(timeoutKey);
            }
        }, Instant.now().plus(timeout));
        
        scheduledTimeouts.put(timeoutKey, scheduledFuture);
    }
    
    private String createTimeoutKey(String orderId) {
        return "timeout-" + orderId;
    }
    
    /**
     * Interface for handling timeout events.
     */
    @FunctionalInterface
    public interface SagaTimeoutHandler {
        void handleTimeout(String orderId, String stepName);
    }
}