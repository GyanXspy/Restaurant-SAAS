package com.restaurant.order.saga;

import com.restaurant.events.publisher.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaDeadLetterQueueHandlerTest {

    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private OrderSagaRepository sagaRepository;
    
    private SagaDeadLetterQueueHandler dlqHandler;
    
    private final String TEST_ORDER_ID = "test-order-123";
    private final String TEST_CUSTOMER_ID = "customer-456";
    
    @BeforeEach
    void setUp() {
        dlqHandler = new SagaDeadLetterQueueHandler(eventPublisher, sagaRepository);
    }
    
    @Test
    void testCartValidationDlqHandling() {
        // Given
        String eventPayload = "{\"orderId\":\"" + TEST_ORDER_ID + "\",\"cartId\":\"cart-123\",\"valid\":true}";
        String errorMessage = "Deserialization failed";
        String topic = "cart-validation-completed-dlq";
        
        OrderSagaData sagaData = createTestSagaData();
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(sagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(sagaData);
        
        // When
        dlqHandler.handleCartValidationDlq(eventPayload, topic, errorMessage);
        
        // Then
        assertEquals(OrderSagaState.SAGA_FAILED, sagaData.getState());
        assertTrue(sagaData.getFailureReason().contains("Cart validation event processing failed"));
        verify(sagaRepository).save(sagaData);
        
        // Verify failed event statistics
        SagaDeadLetterQueueHandler.FailedEventStats stats = dlqHandler.getFailedEventStats();
        assertEquals(1, stats.getTotalFailed());
        assertEquals(0, stats.getReprocessed());
        assertEquals(1, stats.getPending());
    }
    
    @Test
    void testPaymentProcessingDlqHandling() {
        // Given
        String eventPayload = "{\"orderId\":\"" + TEST_ORDER_ID + "\",\"paymentId\":\"payment-123\",\"status\":\"COMPLETED\"}";
        String errorMessage = "JSON parsing error";
        String topic = "payment-processing-completed-dlq";
        
        OrderSagaData sagaData = createTestSagaData();
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(sagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(sagaData);
        
        // When
        dlqHandler.handlePaymentProcessingDlq(eventPayload, topic, errorMessage);
        
        // Then
        assertEquals(OrderSagaState.SAGA_FAILED, sagaData.getState());
        assertTrue(sagaData.getFailureReason().contains("Payment processing event processing failed"));
        verify(sagaRepository).save(sagaData);
    }
    
    @Test
    void testSagaEventsDlqHandling() {
        // Given
        String eventPayload = "{\"orderId\":\"" + TEST_ORDER_ID + "\",\"eventType\":\"OrderCreated\"}";
        String errorMessage = "Event processing timeout";
        String topic = "saga-events-dlq";
        
        OrderSagaData sagaData = createTestSagaData();
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(sagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(sagaData);
        
        // When
        dlqHandler.handleSagaEventsDlq(eventPayload, topic, errorMessage);
        
        // Then
        assertEquals(OrderSagaState.SAGA_FAILED, sagaData.getState());
        assertTrue(sagaData.getFailureReason().contains("Saga event processing failed"));
        verify(sagaRepository).save(sagaData);
    }
    
    @Test
    void testDlqHandlingWithMissingOrderId() {
        // Given
        String eventPayload = "{\"customerId\":\"customer-123\",\"valid\":true}"; // No orderId
        String errorMessage = "Missing order ID";
        String topic = "cart-validation-completed-dlq";
        
        // When
        dlqHandler.handleCartValidationDlq(eventPayload, topic, errorMessage);
        
        // Then
        // Should not attempt to update saga since order ID cannot be extracted
        verify(sagaRepository, never()).findByOrderId(any());
        verify(sagaRepository, never()).save(any());
        
        // But should still record the failed event
        SagaDeadLetterQueueHandler.FailedEventStats stats = dlqHandler.getFailedEventStats();
        assertEquals(1, stats.getTotalFailed());
    }
    
    @Test
    void testDlqHandlingWithNonExistentSaga() {
        // Given
        String eventPayload = "{\"orderId\":\"" + TEST_ORDER_ID + "\",\"cartId\":\"cart-123\",\"valid\":true}";
        String errorMessage = "Processing failed";
        String topic = "cart-validation-completed-dlq";
        
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.empty());
        
        // When
        dlqHandler.handleCartValidationDlq(eventPayload, topic, errorMessage);
        
        // Then
        verify(sagaRepository).findByOrderId(TEST_ORDER_ID);
        verify(sagaRepository, never()).save(any()); // Should not save if saga doesn't exist
        
        // Should still record the failed event
        SagaDeadLetterQueueHandler.FailedEventStats stats = dlqHandler.getFailedEventStats();
        assertEquals(1, stats.getTotalFailed());
    }
    
    @Test
    void testFailedEventReprocessing() {
        // Given
        String eventPayload = "{\"orderId\":\"" + TEST_ORDER_ID + "\",\"cartId\":\"cart-123\",\"valid\":true}";
        String errorMessage = "Temporary failure";
        String topic = "cart-validation-completed-dlq";
        
        // First, create a failed event
        dlqHandler.handleCartValidationDlq(eventPayload, topic, errorMessage);
        
        SagaDeadLetterQueueHandler.FailedEventStats initialStats = dlqHandler.getFailedEventStats();
        assertEquals(1, initialStats.getTotalFailed());
        assertEquals(0, initialStats.getReprocessed());
        
        // Get the event ID (this is a simplified approach - in real implementation you'd have proper event ID tracking)
        String eventId = "cart-validation-completed-" + System.currentTimeMillis() + "-" + 
                        Integer.toHexString(System.identityHashCode(dlqHandler));
        
        // When - Attempt reprocessing (this is a mock since we can't easily get the actual event ID)
        boolean reprocessResult = dlqHandler.reprocessFailedEvent("mock-event-id");
        
        // Then
        // Since we're using a mock event ID, this will return false
        assertFalse(reprocessResult);
    }
    
    @Test
    void testFailedEventStatistics() {
        // Given
        String eventPayload1 = "{\"orderId\":\"order-1\",\"cartId\":\"cart-123\",\"valid\":true}";
        String eventPayload2 = "{\"orderId\":\"order-2\",\"paymentId\":\"payment-123\",\"status\":\"COMPLETED\"}";
        String eventPayload3 = "{\"orderId\":\"order-3\",\"eventType\":\"OrderCreated\"}";
        
        // When
        dlqHandler.handleCartValidationDlq(eventPayload1, "cart-validation-completed-dlq", "Error 1");
        dlqHandler.handlePaymentProcessingDlq(eventPayload2, "payment-processing-completed-dlq", "Error 2");
        dlqHandler.handleSagaEventsDlq(eventPayload3, "saga-events-dlq", "Error 3");
        
        // Then
        SagaDeadLetterQueueHandler.FailedEventStats stats = dlqHandler.getFailedEventStats();
        assertEquals(3, stats.getTotalFailed());
        assertEquals(0, stats.getReprocessed());
        assertEquals(3, stats.getPending());
    }
    
    @Test
    void testDlqHandlingWithRepositoryException() {
        // Given
        String eventPayload = "{\"orderId\":\"" + TEST_ORDER_ID + "\",\"cartId\":\"cart-123\",\"valid\":true}";
        String errorMessage = "Processing failed";
        String topic = "cart-validation-completed-dlq";
        
        OrderSagaData sagaData = createTestSagaData();
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(sagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenThrow(new RuntimeException("Database error"));
        
        // When - Should not throw exception, should handle gracefully
        assertDoesNotThrow(() -> {
            dlqHandler.handleCartValidationDlq(eventPayload, topic, errorMessage);
        });
        
        // Then
        verify(sagaRepository).findByOrderId(TEST_ORDER_ID);
        verify(sagaRepository).save(sagaData);
        
        // Should still record the failed event
        SagaDeadLetterQueueHandler.FailedEventStats stats = dlqHandler.getFailedEventStats();
        assertEquals(1, stats.getTotalFailed());
    }
    
    @Test
    void testOrderIdExtractionFromComplexPayload() {
        // Given
        String complexPayload = "{" +
            "\"eventId\":\"evt-123\"," +
            "\"timestamp\":\"2023-12-01T10:00:00Z\"," +
            "\"orderId\":\"" + TEST_ORDER_ID + "\"," +
            "\"data\":{\"cartId\":\"cart-123\",\"valid\":true}," +
            "\"metadata\":{\"version\":1}" +
        "}";
        
        OrderSagaData sagaData = createTestSagaData();
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(sagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(sagaData);
        
        // When
        dlqHandler.handleCartValidationDlq(complexPayload, "cart-validation-completed-dlq", "Error");
        
        // Then
        verify(sagaRepository).findByOrderId(TEST_ORDER_ID);
        verify(sagaRepository).save(sagaData);
        assertEquals(OrderSagaState.SAGA_FAILED, sagaData.getState());
    }
    
    private OrderSagaData createTestSagaData() {
        OrderSagaData sagaData = new OrderSagaData();
        sagaData.setOrderId(TEST_ORDER_ID);
        sagaData.setCustomerId(TEST_CUSTOMER_ID);
        sagaData.setState(OrderSagaState.CART_VALIDATION_REQUESTED);
        return sagaData;
    }
}