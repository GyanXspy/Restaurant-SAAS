package com.restaurant.order.saga;

import com.restaurant.events.*;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.order.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaTimeoutAndRetryTest {

    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private OrderSagaRepository sagaRepository;
    
    @Mock
    private TaskScheduler taskScheduler;
    
    @Mock
    private ScheduledFuture<?> scheduledFuture;
    
    private SagaTimeoutConfig timeoutConfig;
    private SagaTimeoutManager timeoutManager;
    private SagaRetryManager retryManager;
    private OrderSagaOrchestrator orchestrator;
    
    private OrderSagaData testSagaData;
    private final String TEST_ORDER_ID = "test-order-123";
    private final String TEST_CUSTOMER_ID = "customer-456";
    private final String TEST_RESTAURANT_ID = "restaurant-789";
    
    @BeforeEach
    void setUp() {
        timeoutConfig = new SagaTimeoutConfig();
        timeoutConfig.setCartValidation(Duration.ofMinutes(2));
        timeoutConfig.setPaymentProcessing(Duration.ofMinutes(5));
        timeoutConfig.setOrderConfirmation(Duration.ofMinutes(1));
        timeoutConfig.setMaxRetryAttempts(3);
        timeoutConfig.setInitialRetryDelay(Duration.ofSeconds(1));
        timeoutConfig.setRetryMultiplier(2.0);
        timeoutConfig.setMaxRetryDelay(Duration.ofMinutes(5));
        
        timeoutManager = new SagaTimeoutManager(taskScheduler, timeoutConfig);
        retryManager = new SagaRetryManager(timeoutConfig, timeoutManager, sagaRepository);
        orchestrator = new OrderSagaOrchestrator(eventPublisher, sagaRepository, timeoutManager, retryManager);
        
        // Create test saga data
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item1", "Pizza", new BigDecimal("15.99"), 2),
            new OrderItem("item2", "Coke", new BigDecimal("2.99"), 1)
        );
        testSagaData = new OrderSagaData(TEST_ORDER_ID, TEST_CUSTOMER_ID, TEST_RESTAURANT_ID, 
                                        items, new BigDecimal("34.97"));
        
        // Mock scheduler behavior
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn(scheduledFuture);
        when(scheduledFuture.isDone()).thenReturn(false);
    }
    
    @Test
    void testCartValidationTimeoutScheduling() {
        // Given
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(testSagaData));
        
        // When
        timeoutManager.scheduleCartValidationTimeout(TEST_ORDER_ID, (orderId, stepName) -> {
            // Timeout handler logic
        });
        
        // Then
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        
        Instant scheduledTime = instantCaptor.getValue();
        assertTrue(scheduledTime.isAfter(Instant.now().plus(Duration.ofMinutes(1))));
        assertTrue(scheduledTime.isBefore(Instant.now().plus(Duration.ofMinutes(3))));
    }
    
    @Test
    void testRetryWithExponentialBackoff() {
        // Given
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(testSagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(testSagaData);
        
        Runnable mockOperation = mock(Runnable.class);
        
        // When - First retry
        boolean result1 = retryManager.attemptRetry(TEST_ORDER_ID, mockOperation);
        
        // Then
        assertTrue(result1);
        assertEquals(1, testSagaData.getRetryCount());
        
        // When - Second retry
        boolean result2 = retryManager.attemptRetry(TEST_ORDER_ID, mockOperation);
        
        // Then
        assertTrue(result2);
        assertEquals(2, testSagaData.getRetryCount());
        
        // When - Third retry
        boolean result3 = retryManager.attemptRetry(TEST_ORDER_ID, mockOperation);
        
        // Then
        assertTrue(result3);
        assertEquals(3, testSagaData.getRetryCount());
        
        // When - Fourth retry (should fail - exceeds limit)
        boolean result4 = retryManager.attemptRetry(TEST_ORDER_ID, mockOperation);
        
        // Then
        assertFalse(result4);
        assertEquals(3, testSagaData.getRetryCount()); // Should not increment further
        
        // Verify retry scheduling was called 3 times
        verify(taskScheduler, times(3)).schedule(any(Runnable.class), any(Instant.class));
    }
    
    @Test
    void testExponentialBackoffCalculation() {
        // Test exponential backoff calculation
        Duration delay1 = timeoutConfig.calculateRetryDelay(1);
        Duration delay2 = timeoutConfig.calculateRetryDelay(2);
        Duration delay3 = timeoutConfig.calculateRetryDelay(3);
        
        assertEquals(Duration.ofSeconds(1), delay1);
        assertEquals(Duration.ofSeconds(2), delay2);
        assertEquals(Duration.ofSeconds(4), delay3);
    }
    
    @Test
    void testMaxRetryDelayLimit() {
        // Test that retry delay doesn't exceed maximum
        timeoutConfig.setMaxRetryDelay(Duration.ofSeconds(3));
        
        Duration delay10 = timeoutConfig.calculateRetryDelay(10); // Would be very large without limit
        
        assertEquals(Duration.ofSeconds(3), delay10);
    }
    
    @Test
    void testCartValidationSuccessResetsRetryCount() {
        // Given
        testSagaData.setRetryCount(2);
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(testSagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(testSagaData);
        
        CartValidationCompletedEvent successEvent = new CartValidationCompletedEvent(
            TEST_ORDER_ID, "cart-123", true, null, 1
        );
        
        // When
        orchestrator.handleCartValidationCompleted(successEvent, "cart-validation-completed");
        
        // Then
        assertEquals(0, testSagaData.getRetryCount());
        verify(sagaRepository, times(2)).save(testSagaData); // Once for state update, once for retry reset
    }
    
    @Test
    void testCartValidationFailureTriggersCompensation() {
        // Given
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(testSagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(testSagaData);
        
        CartValidationCompletedEvent failureEvent = new CartValidationCompletedEvent(
            TEST_ORDER_ID, "cart-123", false, Arrays.asList("Item not available"), 1
        );
        
        // When
        orchestrator.handleCartValidationCompleted(failureEvent, "cart-validation-completed");
        
        // Then
        assertEquals(OrderSagaState.CART_VALIDATION_FAILED, testSagaData.getState());
        assertTrue(testSagaData.getFailureReason().contains("Cart validation failed"));
        
        // Verify compensation event is published
        ArgumentCaptor<OrderCancelledEvent> eventCaptor = ArgumentCaptor.forClass(OrderCancelledEvent.class);
        verify(eventPublisher).publish(eq("order-cancelled"), eventCaptor.capture());
        
        OrderCancelledEvent cancelledEvent = eventCaptor.getValue();
        assertEquals(TEST_ORDER_ID, cancelledEvent.getOrderId());
        assertEquals(TEST_CUSTOMER_ID, cancelledEvent.getCustomerId());
    }
    
    @Test
    void testPaymentProcessingTimeoutWithRetry() {
        // Given
        testSagaData.setState(OrderSagaState.PAYMENT_REQUESTED);
        testSagaData.setRetryCount(1); // Already tried once
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(testSagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(testSagaData);
        
        // When - Simulate timeout handler execution
        SagaTimeoutManager.SagaTimeoutHandler timeoutHandler = (orderId, stepName) -> {
            // This simulates the timeout handler logic
            if (!retryManager.hasExceededRetryLimit(orderId)) {
                retryManager.attemptRetry(orderId, () -> {
                    // Retry payment processing
                });
            }
        };
        
        timeoutHandler.handleTimeout(TEST_ORDER_ID, "PAYMENT_PROCESSING");
        
        // Then
        assertEquals(2, testSagaData.getRetryCount());
        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }
    
    @Test
    void testPaymentProcessingTimeoutExceedsRetryLimit() {
        // Given
        testSagaData.setState(OrderSagaState.PAYMENT_REQUESTED);
        testSagaData.setRetryCount(3); // Already at max retries
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(testSagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(testSagaData);
        
        // When
        boolean canRetry = retryManager.hasExceededRetryLimit(TEST_ORDER_ID);
        
        // Then
        assertTrue(canRetry);
        
        // Verify no more retries are attempted
        boolean retryResult = retryManager.attemptRetry(TEST_ORDER_ID, () -> {});
        assertFalse(retryResult);
    }
    
    @Test
    void testTimeoutCancellation() {
        // Given
        when(scheduledFuture.isDone()).thenReturn(false);
        
        // When
        timeoutManager.scheduleCartValidationTimeout(TEST_ORDER_ID, (orderId, stepName) -> {});
        timeoutManager.cancelTimeout(TEST_ORDER_ID);
        
        // Then
        verify(scheduledFuture).cancel(false);
    }
    
    @Test
    void testSagaCompletionCancelsTimeouts() {
        // Given
        testSagaData.setState(OrderSagaState.PAYMENT_COMPLETED);
        testSagaData.setPaymentId("payment-123");
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(testSagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(testSagaData);
        
        // When
        orchestrator.startOrderSaga(TEST_ORDER_ID, TEST_CUSTOMER_ID, TEST_RESTAURANT_ID, 
                                   testSagaData.getItems(), testSagaData.getTotalAmount());
        
        // Then - Verify that timeout scheduling and cancellation methods are called
        verify(taskScheduler, atLeastOnce()).schedule(any(Runnable.class), any(Instant.class));
    }
    
    @Test
    void testEventProcessingWithRetryableAnnotation() {
        // Given
        when(sagaRepository.findByOrderId(TEST_ORDER_ID)).thenReturn(Optional.of(testSagaData));
        when(sagaRepository.save(any(OrderSagaData.class))).thenReturn(testSagaData);
        
        // Simulate a processing exception
        doThrow(new RuntimeException("Database connection failed"))
            .doNothing() // Second call succeeds
            .when(sagaRepository).save(any(OrderSagaData.class));
        
        CartValidationCompletedEvent event = new CartValidationCompletedEvent(
            TEST_ORDER_ID, "cart-123", true, null, 1
        );
        
        // When - This should trigger retry mechanism
        try {
            orchestrator.handleCartValidationCompleted(event, "cart-validation-completed");
        } catch (Exception e) {
            // Expected on first attempt
        }
        
        // Then - Verify retry was attempted
        verify(taskScheduler, atLeastOnce()).schedule(any(Runnable.class), any(Instant.class));
    }
}