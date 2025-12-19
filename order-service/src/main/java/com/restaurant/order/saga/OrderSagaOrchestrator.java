package com.restaurant.order.saga;

import com.restaurant.events.*;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.order.domain.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Saga Orchestrator for managing the complete order processing flow.
 * Implements the Saga pattern to ensure data consistency across distributed services.
 * 
 * Saga Steps:
 * 1. Order Created -> Cart Validation Request
 * 2. Cart Validation Response -> Payment Initiation Request  
 * 3. Payment Response -> Order Confirmation
 * 
 * Each step includes compensating actions for failure scenarios.
 */
@Service
@Transactional
public class OrderSagaOrchestrator {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderSagaOrchestrator.class);
    
    private final EventPublisher eventPublisher;
    private final OrderSagaRepository sagaRepository;
    private final SagaTimeoutManager timeoutManager;
    private final SagaRetryManager retryManager;
    
    public OrderSagaOrchestrator(EventPublisher eventPublisher, 
                                OrderSagaRepository sagaRepository,
                                SagaTimeoutManager timeoutManager,
                                SagaRetryManager retryManager) {
        this.eventPublisher = eventPublisher;
        this.sagaRepository = sagaRepository;
        this.timeoutManager = timeoutManager;
        this.retryManager = retryManager;
    }
    
    /**
     * Starts the order saga orchestration process.
     * Called when a new order is created.
     */
    public void startOrderSaga(String orderId, String customerId, String restaurantId, 
                              List<OrderItem> items, BigDecimal totalAmount) {
        logger.info("Starting order saga for order: {}", orderId);
        
        try {
            // Create and save saga data
            OrderSagaData sagaData = new OrderSagaData(orderId, customerId, restaurantId, items, totalAmount);
            sagaRepository.save(sagaData);
            
            // Publish saga started event
            OrderSagaStartedEvent sagaStartedEvent = new OrderSagaStartedEvent(
                orderId, customerId, restaurantId, 
                convertToEventItems(items), totalAmount, 1
            );
            eventPublisher.publish("order-saga-started", sagaStartedEvent);
            
            // Start cart validation step
            requestCartValidation(sagaData);
            
        } catch (Exception e) {
            logger.error("Failed to start order saga for order: {}", orderId, e);
            handleSagaFailure(orderId, "Failed to start saga: " + e.getMessage());
        }
    }
    
    /**
     * Step 1: Request cart validation
     */
    private void requestCartValidation(OrderSagaData sagaData) {
        logger.info("Requesting cart validation for order: {}", sagaData.getOrderId());
        
        try {
            sagaData.updateState(OrderSagaState.CART_VALIDATION_REQUESTED);
            sagaRepository.save(sagaData);
            
            // Schedule timeout for cart validation
            timeoutManager.scheduleCartValidationTimeout(sagaData.getOrderId(), this::handleCartValidationTimeout);
            
            // Generate cart ID based on customer and restaurant
            String cartId = generateCartId(sagaData.getCustomerId(), sagaData.getRestaurantId());
            
            CartValidationRequestedEvent event = new CartValidationRequestedEvent(
                sagaData.getOrderId(), cartId, sagaData.getCustomerId(), sagaData.getOrderId(), 1
            );
            
            eventPublisher.publish("cart-validation-requested", event);
            logger.info("Cart validation requested for order: {}", sagaData.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to request cart validation for order: {}", sagaData.getOrderId(), e);
            
            // Attempt retry if possible
            if (!retryManager.attemptRetry(sagaData.getOrderId(), () -> requestCartValidation(sagaData))) {
                handleSagaFailure(sagaData.getOrderId(), "Cart validation request failed after retries: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles cart validation response with retry capability
     */
    @RetryableTopic(
        attempts = "1", // We handle retries manually
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = "-dlq"
    )
    @KafkaListener(topics = "cart-validation-completed", groupId = "order-saga-orchestrator")
    public void handleCartValidationCompleted(CartValidationCompletedEvent event,
                                            @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic) {
        logger.info("Received cart validation response for order: {} from topic: {}", event.getOrderId(), topic);
        
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new SagaNotFoundException("Saga not found for order: " + event.getOrderId()));
            
            // Cancel timeout since we received the response
            timeoutManager.cancelTimeout(event.getOrderId());
            
            if (event.isValid()) {
                // Cart validation successful - reset retry count and proceed to payment
                retryManager.resetRetryCount(event.getOrderId());
                sagaData.updateState(OrderSagaState.CART_VALIDATED);
                sagaRepository.save(sagaData);
                
                requestPaymentProcessing(sagaData);
            } else {
                // Cart validation failed - compensate
                logger.warn("Cart validation failed for order: {}, errors: {}", 
                           event.getOrderId(), event.getValidationErrors());
                
                sagaData.updateState(OrderSagaState.CART_VALIDATION_FAILED);
                sagaData.setFailureReason("Cart validation failed: " + 
                    (event.getValidationErrors() != null ? String.join(", ", event.getValidationErrors()) : "Unknown error"));
                sagaRepository.save(sagaData);
                
                compensateCartValidationFailure(sagaData);
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle cart validation response for order: {}", event.getOrderId(), e);
            
            // Attempt retry if possible
            if (!retryManager.attemptRetry(event.getOrderId(), () -> {
                try {
                    handleCartValidationCompleted(event, topic);
                } catch (Exception retryException) {
                    logger.error("Retry failed for cart validation response: {}", event.getOrderId(), retryException);
                }
            })) {
                handleSagaFailure(event.getOrderId(), "Failed to process cart validation response after retries: " + e.getMessage());
            }
        }
    }
    
    /**
     * Step 2: Request payment processing
     */
    private void requestPaymentProcessing(OrderSagaData sagaData) {
        logger.info("Requesting payment processing for order: {}", sagaData.getOrderId());
        
        try {
            sagaData.updateState(OrderSagaState.PAYMENT_REQUESTED);
            String paymentId = UUID.randomUUID().toString();
            sagaData.setPaymentId(paymentId);
            sagaRepository.save(sagaData);
            
            // Schedule timeout for payment processing
            timeoutManager.schedulePaymentProcessingTimeout(sagaData.getOrderId(), this::handlePaymentProcessingTimeout);
            
            PaymentInitiationRequestedEvent event = new PaymentInitiationRequestedEvent(
                sagaData.getOrderId(), paymentId, sagaData.getOrderId(), 
                sagaData.getTotalAmount(), sagaData.getCustomerId(), 1
            );
            
            eventPublisher.publish("payment-initiation-requested", event);
            logger.info("Payment processing requested for order: {}, paymentId: {}", 
                       sagaData.getOrderId(), paymentId);
            
        } catch (Exception e) {
            logger.error("Failed to request payment processing for order: {}", sagaData.getOrderId(), e);
            
            // Attempt retry if possible
            if (!retryManager.attemptRetry(sagaData.getOrderId(), () -> requestPaymentProcessing(sagaData))) {
                handleSagaFailure(sagaData.getOrderId(), "Payment request failed after retries: " + e.getMessage());
            }
        }
    }
    
    /**
     * Handles payment processing response with retry capability
     */
    @RetryableTopic(
        attempts = "1", // We handle retries manually
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
        topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
        dltTopicSuffix = "-dlq"
    )
    @KafkaListener(topics = "payment-processing-completed", groupId = "order-saga-orchestrator")
    public void handlePaymentProcessingCompleted(PaymentProcessingCompletedEvent event,
                                               @Header(value = KafkaHeaders.RECEIVED_TOPIC, required = false) String topic) {
        logger.info("Received payment processing response for order: {} from topic: {}", event.getOrderId(), topic);
        
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(event.getOrderId())
                .orElseThrow(() -> new SagaNotFoundException("Saga not found for order: " + event.getOrderId()));
            
            // Cancel timeout since we received the response
            timeoutManager.cancelTimeout(event.getOrderId());
            
            if (event.getStatus() == PaymentProcessingCompletedEvent.PaymentStatus.COMPLETED) {
                // Payment successful - reset retry count and confirm order
                retryManager.resetRetryCount(event.getOrderId());
                sagaData.updateState(OrderSagaState.PAYMENT_COMPLETED);
                sagaRepository.save(sagaData);
                
                confirmOrder(sagaData);
            } else {
                // Payment failed - compensate
                logger.warn("Payment processing failed for order: {}, reason: {}", 
                           event.getOrderId(), event.getFailureReason());
                
                sagaData.updateState(OrderSagaState.PAYMENT_FAILED);
                sagaData.setFailureReason("Payment failed: " + event.getFailureReason());
                sagaRepository.save(sagaData);
                
                compensatePaymentFailure(sagaData);
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle payment processing response for order: {}", event.getOrderId(), e);
            
            // Attempt retry if possible
            if (!retryManager.attemptRetry(event.getOrderId(), () -> {
                try {
                    handlePaymentProcessingCompleted(event, topic);
                } catch (Exception retryException) {
                    logger.error("Retry failed for payment processing response: {}", event.getOrderId(), retryException);
                }
            })) {
                handleSagaFailure(event.getOrderId(), "Failed to process payment response after retries: " + e.getMessage());
            }
        }
    }
    
    /**
     * Step 3: Confirm order
     */
    private void confirmOrder(OrderSagaData sagaData) {
        logger.info("Confirming order: {}", sagaData.getOrderId());
        
        try {
            sagaData.updateState(OrderSagaState.ORDER_CONFIRMED);
            sagaRepository.save(sagaData);
            
            // Schedule timeout for order confirmation
            timeoutManager.scheduleOrderConfirmationTimeout(sagaData.getOrderId(), this::handleOrderConfirmationTimeout);
            
            OrderConfirmedEvent event = new OrderConfirmedEvent(
                sagaData.getOrderId(), sagaData.getCustomerId(), sagaData.getRestaurantId(),
                sagaData.getTotalAmount(), sagaData.getPaymentId(), 1
            );
            
            eventPublisher.publish("order-confirmed", event);
            
            // Mark saga as completed and cancel any remaining timeouts
            timeoutManager.cancelTimeout(sagaData.getOrderId());
            sagaData.updateState(OrderSagaState.SAGA_COMPLETED);
            sagaRepository.save(sagaData);
            
            logger.info("Order saga completed successfully for order: {}", sagaData.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to confirm order: {}", sagaData.getOrderId(), e);
            
            // Attempt retry if possible
            if (!retryManager.attemptRetry(sagaData.getOrderId(), () -> confirmOrder(sagaData))) {
                handleSagaFailure(sagaData.getOrderId(), "Order confirmation failed after retries: " + e.getMessage());
            }
        }
    }
    
    // Compensating Actions
    
    /**
     * Compensating action for cart validation failure
     */
    private void compensateCartValidationFailure(OrderSagaData sagaData) {
        logger.info("Compensating cart validation failure for order: {}", sagaData.getOrderId());
        
        try {
            sagaData.updateState(OrderSagaState.COMPENSATING_CART_VALIDATION);
            sagaRepository.save(sagaData);
            
            // Cancel the order
            OrderCancelledEvent event = new OrderCancelledEvent(
                sagaData.getOrderId(), sagaData.getCustomerId(), 
                sagaData.getFailureReason(), 1
            );
            
            eventPublisher.publish("order-cancelled", event);
            
            sagaData.updateState(OrderSagaState.SAGA_FAILED);
            sagaRepository.save(sagaData);
            
            logger.info("Cart validation failure compensated for order: {}", sagaData.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to compensate cart validation failure for order: {}", 
                        sagaData.getOrderId(), e);
        }
    }
    
    /**
     * Compensating action for payment failure
     */
    private void compensatePaymentFailure(OrderSagaData sagaData) {
        logger.info("Compensating payment failure for order: {}", sagaData.getOrderId());
        
        try {
            sagaData.updateState(OrderSagaState.COMPENSATING_PAYMENT);
            sagaRepository.save(sagaData);
            
            // Release cart items (publish event to cart service)
            // Note: This would be a new event type for releasing cart items
            logger.info("Cart items released for order: {}", sagaData.getOrderId());
            
            // Cancel the order
            OrderCancelledEvent event = new OrderCancelledEvent(
                sagaData.getOrderId(), sagaData.getCustomerId(), 
                sagaData.getFailureReason(), 1
            );
            
            eventPublisher.publish("order-cancelled", event);
            
            sagaData.updateState(OrderSagaState.SAGA_FAILED);
            sagaRepository.save(sagaData);
            
            logger.info("Payment failure compensated for order: {}", sagaData.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to compensate payment failure for order: {}", 
                        sagaData.getOrderId(), e);
        }
    }
    
    /**
     * Handles general saga failures
     */
    private void handleSagaFailure(String orderId, String reason) {
        logger.error("Saga failed for order: {}, reason: {}", orderId, reason);
        
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(orderId)
                .orElse(null);
            
            if (sagaData != null) {
                sagaData.updateState(OrderSagaState.SAGA_FAILED);
                sagaData.setFailureReason(reason);
                sagaRepository.save(sagaData);
            }
            
            // Publish order cancelled event
            OrderCancelledEvent event = new OrderCancelledEvent(orderId, null, reason, 1);
            eventPublisher.publish("order-cancelled", event);
            
        } catch (Exception e) {
            logger.error("Failed to handle saga failure for order: {}", orderId, e);
        }
    }
    
    // Utility methods
    
    private String generateCartId(String customerId, String restaurantId) {
        return customerId + "-" + restaurantId + "-cart";
    }
    
    private List<OrderCreatedEvent.OrderItem> convertToEventItems(List<OrderItem> items) {
        return items.stream()
            .map(item -> new OrderCreatedEvent.OrderItem(
                item.getItemId(), item.getName(), item.getPrice(), item.getQuantity()
            ))
            .toList();
    }
    
    // Timeout Handlers
    
    /**
     * Handles timeout for cart validation step.
     */
    private void handleCartValidationTimeout(String orderId, String stepName) {
        logger.warn("Cart validation timeout for order: {} in step: {}", orderId, stepName);
        
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(orderId).orElse(null);
            if (sagaData == null) {
                logger.error("Saga not found for timeout handling: {}", orderId);
                return;
            }
            
            // Check if we can retry
            if (!retryManager.hasExceededRetryLimit(orderId)) {
                logger.info("Retrying cart validation for order: {} due to timeout", orderId);
                retryManager.attemptRetry(orderId, () -> requestCartValidation(sagaData));
            } else {
                logger.error("Cart validation timeout exceeded retry limit for order: {}", orderId);
                sagaData.updateState(OrderSagaState.CART_VALIDATION_FAILED);
                sagaData.setFailureReason("Cart validation timeout after " + sagaData.getRetryCount() + " retries");
                sagaRepository.save(sagaData);
                
                compensateCartValidationFailure(sagaData);
            }
        } catch (Exception e) {
            logger.error("Error handling cart validation timeout for order: {}", orderId, e);
            handleSagaFailure(orderId, "Cart validation timeout handling failed: " + e.getMessage());
        }
    }
    
    /**
     * Handles timeout for payment processing step.
     */
    private void handlePaymentProcessingTimeout(String orderId, String stepName) {
        logger.warn("Payment processing timeout for order: {} in step: {}", orderId, stepName);
        
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(orderId).orElse(null);
            if (sagaData == null) {
                logger.error("Saga not found for timeout handling: {}", orderId);
                return;
            }
            
            // Check if we can retry
            if (!retryManager.hasExceededRetryLimit(orderId)) {
                logger.info("Retrying payment processing for order: {} due to timeout", orderId);
                retryManager.attemptRetry(orderId, () -> requestPaymentProcessing(sagaData));
            } else {
                logger.error("Payment processing timeout exceeded retry limit for order: {}", orderId);
                sagaData.updateState(OrderSagaState.PAYMENT_FAILED);
                sagaData.setFailureReason("Payment processing timeout after " + sagaData.getRetryCount() + " retries");
                sagaRepository.save(sagaData);
                
                compensatePaymentFailure(sagaData);
            }
        } catch (Exception e) {
            logger.error("Error handling payment processing timeout for order: {}", orderId, e);
            handleSagaFailure(orderId, "Payment processing timeout handling failed: " + e.getMessage());
        }
    }
    
    /**
     * Handles timeout for order confirmation step.
     */
    private void handleOrderConfirmationTimeout(String orderId, String stepName) {
        logger.warn("Order confirmation timeout for order: {} in step: {}", orderId, stepName);
        
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(orderId).orElse(null);
            if (sagaData == null) {
                logger.error("Saga not found for timeout handling: {}", orderId);
                return;
            }
            
            // Check if we can retry
            if (!retryManager.hasExceededRetryLimit(orderId)) {
                logger.info("Retrying order confirmation for order: {} due to timeout", orderId);
                retryManager.attemptRetry(orderId, () -> confirmOrder(sagaData));
            } else {
                logger.error("Order confirmation timeout exceeded retry limit for order: {}", orderId);
                sagaData.updateState(OrderSagaState.SAGA_FAILED);
                sagaData.setFailureReason("Order confirmation timeout after " + sagaData.getRetryCount() + " retries");
                sagaRepository.save(sagaData);
                
                // For order confirmation timeout, we might need to compensate payment
                compensatePaymentFailure(sagaData);
            }
        } catch (Exception e) {
            logger.error("Error handling order confirmation timeout for order: {}", orderId, e);
            handleSagaFailure(orderId, "Order confirmation timeout handling failed: " + e.getMessage());
        }
    }
}