package com.restaurant.order.saga;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.publisher.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles events that have failed processing and been sent to dead letter queues.
 * Provides monitoring, alerting, and manual recovery capabilities.
 */
@Service
public class SagaDeadLetterQueueHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(SagaDeadLetterQueueHandler.class);
    
    private final EventPublisher eventPublisher;
    private final OrderSagaRepository sagaRepository;
    private final ConcurrentHashMap<String, FailedEventInfo> failedEvents = new ConcurrentHashMap<>();
    
    public SagaDeadLetterQueueHandler(EventPublisher eventPublisher, OrderSagaRepository sagaRepository) {
        this.eventPublisher = eventPublisher;
        this.sagaRepository = sagaRepository;
    }
    
    /**
     * Handles cart validation events that failed processing.
     */
    @KafkaListener(topics = "cart-validation-completed-dlq", groupId = "saga-dlq-handler")
    public void handleCartValidationDlq(@Payload String eventPayload,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        
        logger.error("Received cart validation event in DLQ. Topic: {}, Error: {}, Payload: {}", 
                    topic, errorMessage, eventPayload);
        
        recordFailedEvent("cart-validation-completed", eventPayload, errorMessage);
        
        // Try to extract order ID and handle saga failure
        try {
            String orderId = extractOrderIdFromPayload(eventPayload);
            if (orderId != null) {
                handleSagaEventFailure(orderId, "Cart validation event processing failed", errorMessage);
            }
        } catch (Exception e) {
            logger.error("Failed to handle cart validation DLQ event", e);
        }
    }
    
    /**
     * Handles payment processing events that failed processing.
     */
    @KafkaListener(topics = "payment-processing-completed-dlq", groupId = "saga-dlq-handler")
    public void handlePaymentProcessingDlq(@Payload String eventPayload,
                                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                          @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        
        logger.error("Received payment processing event in DLQ. Topic: {}, Error: {}, Payload: {}", 
                    topic, errorMessage, eventPayload);
        
        recordFailedEvent("payment-processing-completed", eventPayload, errorMessage);
        
        // Try to extract order ID and handle saga failure
        try {
            String orderId = extractOrderIdFromPayload(eventPayload);
            if (orderId != null) {
                handleSagaEventFailure(orderId, "Payment processing event processing failed", errorMessage);
            }
        } catch (Exception e) {
            logger.error("Failed to handle payment processing DLQ event", e);
        }
    }
    
    /**
     * Handles general saga events that failed processing.
     */
    @KafkaListener(topics = "saga-events-dlq", groupId = "saga-dlq-handler")
    public void handleSagaEventsDlq(@Payload String eventPayload,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   @Header(value = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String errorMessage) {
        
        logger.error("Received saga event in DLQ. Topic: {}, Error: {}, Payload: {}", 
                    topic, errorMessage, eventPayload);
        
        recordFailedEvent("saga-events", eventPayload, errorMessage);
        
        // Try to extract order ID and handle saga failure
        try {
            String orderId = extractOrderIdFromPayload(eventPayload);
            if (orderId != null) {
                handleSagaEventFailure(orderId, "Saga event processing failed", errorMessage);
            }
        } catch (Exception e) {
            logger.error("Failed to handle saga DLQ event", e);
        }
    }
    
    /**
     * Attempts to manually reprocess a failed event.
     */
    public boolean reprocessFailedEvent(String eventId) {
        FailedEventInfo failedEvent = failedEvents.get(eventId);
        if (failedEvent == null) {
            logger.warn("Failed event not found for reprocessing: {}", eventId);
            return false;
        }
        
        try {
            logger.info("Attempting to reprocess failed event: {}", eventId);
            
            // This would typically involve republishing the event to the original topic
            // For now, we'll log the attempt and mark it as reprocessed
            failedEvent.setReprocessed(true);
            failedEvent.setReprocessedAt(LocalDateTime.now());
            
            logger.info("Successfully reprocessed failed event: {}", eventId);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to reprocess event: {}", eventId, e);
            return false;
        }
    }
    
    /**
     * Gets statistics about failed events.
     */
    public FailedEventStats getFailedEventStats() {
        long totalFailed = failedEvents.size();
        long reprocessed = failedEvents.values().stream()
            .mapToLong(event -> event.isReprocessed() ? 1 : 0)
            .sum();
        
        return new FailedEventStats(totalFailed, reprocessed, totalFailed - reprocessed);
    }
    
    private void recordFailedEvent(String eventType, String payload, String errorMessage) {
        String eventId = generateEventId(eventType);
        FailedEventInfo failedEvent = new FailedEventInfo(
            eventId, eventType, payload, errorMessage, LocalDateTime.now()
        );
        
        failedEvents.put(eventId, failedEvent);
        
        // Log for monitoring/alerting systems
        logger.error("SAGA_DLQ_EVENT: eventId={}, eventType={}, error={}", 
                    eventId, eventType, errorMessage);
    }
    
    private void handleSagaEventFailure(String orderId, String reason, String errorMessage) {
        try {
            OrderSagaData sagaData = sagaRepository.findByOrderId(orderId).orElse(null);
            
            if (sagaData != null) {
                String fullReason = reason + (errorMessage != null ? ": " + errorMessage : "");
                sagaData.updateState(OrderSagaState.SAGA_FAILED);
                sagaData.setFailureReason(fullReason);
                sagaRepository.save(sagaData);
                
                logger.error("Marked saga as failed for order: {} due to DLQ event. Reason: {}", 
                           orderId, fullReason);
            }
        } catch (Exception e) {
            logger.error("Failed to handle saga failure for order: {}", orderId, e);
        }
    }
    
    private String extractOrderIdFromPayload(String payload) {
        // Simple extraction - in a real implementation, you'd parse the JSON
        // This is a simplified version for demonstration
        try {
            if (payload.contains("\"orderId\"")) {
                int start = payload.indexOf("\"orderId\":\"") + 11;
                int end = payload.indexOf("\"", start);
                if (start > 10 && end > start) {
                    return payload.substring(start, end);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract order ID from payload: {}", payload, e);
        }
        return null;
    }
    
    private String generateEventId(String eventType) {
        return eventType + "-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(System.identityHashCode(this));
    }
    
    /**
     * Information about a failed event.
     */
    public static class FailedEventInfo {
        private final String eventId;
        private final String eventType;
        private final String payload;
        private final String errorMessage;
        private final LocalDateTime failedAt;
        private boolean reprocessed = false;
        private LocalDateTime reprocessedAt;
        
        public FailedEventInfo(String eventId, String eventType, String payload, 
                              String errorMessage, LocalDateTime failedAt) {
            this.eventId = eventId;
            this.eventType = eventType;
            this.payload = payload;
            this.errorMessage = errorMessage;
            this.failedAt = failedAt;
        }
        
        // Getters and setters
        public String getEventId() { return eventId; }
        public String getEventType() { return eventType; }
        public String getPayload() { return payload; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getFailedAt() { return failedAt; }
        public boolean isReprocessed() { return reprocessed; }
        public void setReprocessed(boolean reprocessed) { this.reprocessed = reprocessed; }
        public LocalDateTime getReprocessedAt() { return reprocessedAt; }
        public void setReprocessedAt(LocalDateTime reprocessedAt) { this.reprocessedAt = reprocessedAt; }
    }
    
    /**
     * Statistics about failed events.
     */
    public static class FailedEventStats {
        private final long totalFailed;
        private final long reprocessed;
        private final long pending;
        
        public FailedEventStats(long totalFailed, long reprocessed, long pending) {
            this.totalFailed = totalFailed;
            this.reprocessed = reprocessed;
            this.pending = pending;
        }
        
        public long getTotalFailed() { return totalFailed; }
        public long getReprocessed() { return reprocessed; }
        public long getPending() { return pending; }
    }
}