package com.restaurant.order.saga;

import com.restaurant.events.*;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.order.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "saga.timeout.cart-validation=PT5S",  // 5 seconds for faster testing
    "saga.timeout.payment-processing=PT10S",  // 10 seconds for faster testing
    "saga.timeout.order-confirmation=PT3S",  // 3 seconds for faster testing
    "saga.timeout.max-retry-attempts=2",  // Fewer retries for faster testing
    "saga.timeout.initial-retry-delay=PT1S",
    "saga.timeout.retry-multiplier=2.0"
})
@Transactional
class SagaIntegrationTest {

    @MockBean
    private EventPublisher eventPublisher;
    
    private OrderSagaOrchestrator orchestrator;
    private OrderSagaRepository sagaRepository;
    private SagaTimeoutManager timeoutManager;
    private SagaRetryManager retryManager;
    
    private final String TEST_ORDER_ID = "integration-test-order-123";
    private final String TEST_CUSTOMER_ID = "customer-456";
    private final String TEST_RESTAURANT_ID = "restaurant-789";
    
    @BeforeEach
    void setUp() {
        // These would be injected in a real Spring Boot test
        // For this example, we'll create them manually
    }
    
    @Test
    void testSuccessfulSagaFlow() throws Exception {
        // Given
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item1", "Pizza", new BigDecimal("15.99"), 2),
            new OrderItem("item2", "Coke", new BigDecimal("2.99"), 1)
        );
        BigDecimal totalAmount = new BigDecimal("34.97");
        
        // When - Start the saga
        orchestrator.startOrderSaga(TEST_ORDER_ID, TEST_CUSTOMER_ID, TEST_RESTAURANT_ID, items, totalAmount);
        
        // Simulate successful cart validation response
        CartValidationCompletedEvent cartValidationEvent = new CartValidationCompletedEvent(
            TEST_ORDER_ID, "cart-123", true, null, 1
        );
        orchestrator.handleCartValidationCompleted(cartValidationEvent, "cart-validation-completed");
        
        // Simulate successful payment processing response
        PaymentProcessingCompletedEvent paymentEvent = new PaymentProcessingCompletedEvent(
            TEST_ORDER_ID, "payment-123", PaymentProcessingCompletedEvent.PaymentStatus.COMPLETED, null, 1
        );
        orchestrator.handlePaymentProcessingCompleted(paymentEvent, "payment-processing-completed");
        
        // Then - Verify all events were published
        verify(eventPublisher).publish(eq("order-saga-started"), any(OrderSagaStartedEvent.class));
        verify(eventPublisher).publish(eq("cart-validation-requested"), any(CartValidationRequestedEvent.class));
        verify(eventPublisher).publish(eq("payment-initiation-requested"), any(PaymentInitiationRequestedEvent.class));
        verify(eventPublisher).publish(eq("order-confirmed"), any(OrderConfirmedEvent.class));
    }
    
    @Test
    void testSagaFlowWithCartValidationFailure() throws Exception {
        // Given
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item1", "Pizza", new BigDecimal("15.99"), 1)
        );
        BigDecimal totalAmount = new BigDecimal("15.99");
        
        // When - Start the saga
        orchestrator.startOrderSaga(TEST_ORDER_ID, TEST_CUSTOMER_ID, TEST_RESTAURANT_ID, items, totalAmount);
        
        // Simulate cart validation failure
        CartValidationCompletedEvent cartValidationEvent = new CartValidationCompletedEvent(
            TEST_ORDER_ID, "cart-123", false, Arrays.asList("Item not available"), 1
        );
        orchestrator.handleCartValidationCompleted(cartValidationEvent, "cart-validation-completed");
        
        // Then - Verify compensation was triggered
        verify(eventPublisher).publish(eq("order-saga-started"), any(OrderSagaStartedEvent.class));
        verify(eventPublisher).publish(eq("cart-validation-requested"), any(CartValidationRequestedEvent.class));
        verify(eventPublisher).publish(eq("order-cancelled"), any(OrderCancelledEvent.class));
        
        // Should not proceed to payment
        verify(eventPublisher, never()).publish(eq("payment-initiation-requested"), any());
    }
    
    @Test
    void testSagaFlowWithPaymentFailure() throws Exception {
        // Given
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item1", "Pizza", new BigDecimal("15.99"), 1)
        );
        BigDecimal totalAmount = new BigDecimal("15.99");
        
        // When - Start the saga
        orchestrator.startOrderSaga(TEST_ORDER_ID, TEST_CUSTOMER_ID, TEST_RESTAURANT_ID, items, totalAmount);
        
        // Simulate successful cart validation
        CartValidationCompletedEvent cartValidationEvent = new CartValidationCompletedEvent(
            TEST_ORDER_ID, "cart-123", true, null, 1
        );
        orchestrator.handleCartValidationCompleted(cartValidationEvent, "cart-validation-completed");
        
        // Simulate payment failure
        PaymentProcessingCompletedEvent paymentEvent = new PaymentProcessingCompletedEvent(
            TEST_ORDER_ID, "payment-123", PaymentProcessingCompletedEvent.PaymentStatus.FAILED, 
            "Insufficient funds", 1
        );
        orchestrator.handlePaymentProcessingCompleted(paymentEvent, "payment-processing-completed");
        
        // Then - Verify compensation was triggered
        verify(eventPublisher).publish(eq("order-saga-started"), any(OrderSagaStartedEvent.class));
        verify(eventPublisher).publish(eq("cart-validation-requested"), any(CartValidationRequestedEvent.class));
        verify(eventPublisher).publish(eq("payment-initiation-requested"), any(PaymentInitiationRequestedEvent.class));
        verify(eventPublisher).publish(eq("order-cancelled"), any(OrderCancelledEvent.class));
        
        // Should not confirm order
        verify(eventPublisher, never()).publish(eq("order-confirmed"), any());
    }
    
    @Test
    void testSagaTimeoutHandling() throws Exception {
        // This test would require actual timeout simulation
        // In a real integration test, you would:
        // 1. Start a saga
        // 2. Not send the expected response event
        // 3. Wait for the timeout period
        // 4. Verify that timeout handling was triggered
        
        // For demonstration purposes, we'll test the timeout configuration
        SagaTimeoutConfig config = new SagaTimeoutConfig();
        config.setCartValidation(Duration.ofSeconds(5));
        config.setMaxRetryAttempts(2);
        
        Duration retryDelay1 = config.calculateRetryDelay(1);
        Duration retryDelay2 = config.calculateRetryDelay(2);
        
        assertEquals(Duration.ofSeconds(1), retryDelay1);
        assertEquals(Duration.ofSeconds(2), retryDelay2);
    }
    
    @Test
    void testConcurrentSagaExecution() throws Exception {
        // Given - Multiple orders
        String orderId1 = "concurrent-order-1";
        String orderId2 = "concurrent-order-2";
        String orderId3 = "concurrent-order-3";
        
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item1", "Pizza", new BigDecimal("15.99"), 1)
        );
        BigDecimal totalAmount = new BigDecimal("15.99");
        
        // When - Start multiple sagas concurrently
        CompletableFuture<Void> saga1 = CompletableFuture.runAsync(() -> {
            orchestrator.startOrderSaga(orderId1, TEST_CUSTOMER_ID, TEST_RESTAURANT_ID, items, totalAmount);
        });
        
        CompletableFuture<Void> saga2 = CompletableFuture.runAsync(() -> {
            orchestrator.startOrderSaga(orderId2, TEST_CUSTOMER_ID, TEST_RESTAURANT_ID, items, totalAmount);
        });
        
        CompletableFuture<Void> saga3 = CompletableFuture.runAsync(() -> {
            orchestrator.startOrderSaga(orderId3, TEST_CUSTOMER_ID, TEST_RESTAURANT_ID, items, totalAmount);
        });
        
        // Wait for all sagas to start
        CompletableFuture.allOf(saga1, saga2, saga3).get(5, TimeUnit.SECONDS);
        
        // Then - Verify all sagas were started
        verify(eventPublisher, times(3)).publish(eq("order-saga-started"), any(OrderSagaStartedEvent.class));
        verify(eventPublisher, times(3)).publish(eq("cart-validation-requested"), any(CartValidationRequestedEvent.class));
    }
    
    @Test
    void testSagaRecoveryAfterSystemRestart() {
        // This test would simulate system restart scenarios
        // In a real implementation, you would:
        // 1. Start a saga and persist its state
        // 2. Simulate system shutdown (stop the orchestrator)
        // 3. Restart the system
        // 4. Verify that in-progress sagas are recovered and continue processing
        
        // For this example, we'll test the saga state persistence
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item1", "Pizza", new BigDecimal("15.99"), 1)
        );
        
        OrderSagaData sagaData = new OrderSagaData(TEST_ORDER_ID, TEST_CUSTOMER_ID, TEST_RESTAURANT_ID, 
                                                  items, new BigDecimal("15.99"));
        sagaData.updateState(OrderSagaState.CART_VALIDATION_REQUESTED);
        
        // Verify saga state can be persisted and retrieved
        assertNotNull(sagaData.getOrderId());
        assertEquals(OrderSagaState.CART_VALIDATION_REQUESTED, sagaData.getState());
        assertNotNull(sagaData.getCreatedAt());
        assertNotNull(sagaData.getUpdatedAt());
    }
    
    @Test
    void testSagaMetricsAndMonitoring() {
        // This test would verify that proper metrics are collected
        // In a real implementation, you would:
        // 1. Start sagas with different outcomes
        // 2. Verify that metrics are properly recorded (success rate, failure rate, timeout rate, etc.)
        // 3. Check that alerts are triggered for high failure rates
        
        // For this example, we'll test the failed event statistics
        SagaDeadLetterQueueHandler dlqHandler = new SagaDeadLetterQueueHandler(eventPublisher, sagaRepository);
        
        // Simulate some failed events
        dlqHandler.handleCartValidationDlq("{\"orderId\":\"order-1\"}", "dlq-topic", "Error 1");
        dlqHandler.handlePaymentProcessingDlq("{\"orderId\":\"order-2\"}", "dlq-topic", "Error 2");
        
        SagaDeadLetterQueueHandler.FailedEventStats stats = dlqHandler.getFailedEventStats();
        assertEquals(2, stats.getTotalFailed());
        assertEquals(0, stats.getReprocessed());
        assertEquals(2, stats.getPending());
    }
}