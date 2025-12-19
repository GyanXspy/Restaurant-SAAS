package com.restaurant.order.saga;

import com.restaurant.events.CartValidationCompletedEvent;
import com.restaurant.events.PaymentProcessingCompletedEvent;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.order.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaPerformanceTest {

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
    
    private ExecutorService executorService;
    
    @BeforeEach
    void setUp() {
        timeoutConfig = new SagaTimeoutConfig();
        timeoutConfig.setCartValidation(Duration.ofSeconds(1));
        timeoutConfig.setPaymentProcessing(Duration.ofSeconds(2));
        timeoutConfig.setOrderConfirmation(Duration.ofSeconds(1));
        timeoutConfig.setMaxRetryAttempts(3);
        timeoutConfig.setInitialRetryDelay(Duration.ofMillis(100));
        timeoutConfig.setRetryMultiplier(2.0);
        timeoutConfig.setMaxRetryDelay(Duration.ofSeconds(5));
        
        timeoutManager = new SagaTimeoutManager(taskScheduler, timeoutConfig);
        retryManager = new SagaRetryManager(timeoutConfig, timeoutManager, sagaRepository);
        orchestrator = new OrderSagaOrchestrator(eventPublisher, sagaRepository, timeoutManager, retryManager);
        
        executorService = Executors.newFixedThreadPool(10);
        
        // Mock scheduler behavior
        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class))).thenReturn(scheduledFuture);
        when(scheduledFuture.isDone()).thenReturn(false);
    }
    
    @Test
    void testHighVolumeOrderProcessing() throws Exception {
        // Given
        int numberOfOrders = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfOrders);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item1", "Pizza", new BigDecimal("15.99"), 1)
        );
        BigDecimal totalAmount = new BigDecimal("15.99");
        
        // Mock repository to return saga data
        when(sagaRepository.findByOrderId(any())).thenAnswer(invocation -> {
            String orderId = invocation.getArgument(0);
            OrderSagaData sagaData = new OrderSagaData(orderId, "customer-123", "restaurant-456", items, totalAmount);
            return Optional.of(sagaData);
        });
        when(sagaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When - Process orders concurrently
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfOrders; i++) {
            final String orderId = "perf-test-order-" + i;
            
            executorService.submit(() -> {
                try {
                    orchestrator.startOrderSaga(orderId, "customer-123", "restaurant-456", items, totalAmount);
                    
                    // Simulate successful cart validation
                    CartValidationCompletedEvent cartEvent = new CartValidationCompletedEvent(
                        orderId, "cart-" + orderId, true, null, 1
                    );
                    orchestrator.handleCartValidationCompleted(cartEvent, "cart-validation-completed");
                    
                    // Simulate successful payment
                    PaymentProcessingCompletedEvent paymentEvent = new PaymentProcessingCompletedEvent(
                        orderId, "payment-" + orderId, PaymentProcessingCompletedEvent.PaymentStatus.COMPLETED, null, 1
                    );
                    orchestrator.handlePaymentProcessingCompleted(paymentEvent, "payment-processing-completed");
                    
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all orders to complete (with timeout)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertTrue(completed, "Not all orders completed within timeout");
        
        long processingTime = endTime - startTime;
        double ordersPerSecond = (double) numberOfOrders / (processingTime / 1000.0);
        
        System.out.println("Performance Test Results:");
        System.out.println("Total Orders: " + numberOfOrders);
        System.out.println("Processing Time: " + processingTime + "ms");
        System.out.println("Orders per Second: " + String.format("%.2f", ordersPerSecond));
        System.out.println("Success Count: " + successCount.get());
        System.out.println("Failure Count: " + failureCount.get());
        
        // Verify performance expectations
        assertTrue(ordersPerSecond > 50, "Processing rate should be > 50 orders/second, was: " + ordersPerSecond);
        assertEquals(numberOfOrders, successCount.get(), "All orders should succeed");
        assertEquals(0, failureCount.get(), "No orders should fail");
        
        // Verify event publishing
        verify(eventPublisher, times(numberOfOrders)).publish(eq("order-saga-started"), any());
        verify(eventPublisher, times(numberOfOrders)).publish(eq("cart-validation-requested"), any());
        verify(eventPublisher, times(numberOfOrders)).publish(eq("payment-initiation-requested"), any());
        verify(eventPublisher, times(numberOfOrders)).publish(eq("order-confirmed"), any());
    }
    
    @Test
    void testRetryPerformanceUnderLoad() throws Exception {
        // Given
        int numberOfOrders = 100;
        CountDownLatch latch = new CountDownLatch(numberOfOrders);
        AtomicInteger retryCount = new AtomicInteger(0);
        
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item1", "Pizza", new BigDecimal("15.99"), 1)
        );
        BigDecimal totalAmount = new BigDecimal("15.99");
        
        // Mock repository to simulate retry scenarios
        when(sagaRepository.findByOrderId(any())).thenAnswer(invocation -> {
            String orderId = invocation.getArgument(0);
            OrderSagaData sagaData = new OrderSagaData(orderId, "customer-123", "restaurant-456", items, totalAmount);
            sagaData.setRetryCount(retryCount.get() % 3); // Vary retry count
            return Optional.of(sagaData);
        });
        when(sagaRepository.save(any())).thenAnswer(invocation -> {
            OrderSagaData sagaData = invocation.getArgument(0);
            retryCount.incrementAndGet();
            return sagaData;
        });
        
        // When - Test retry performance
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfOrders; i++) {
            final String orderId = "retry-test-order-" + i;
            
            executorService.submit(() -> {
                try {
                    // Simulate retry scenarios
                    boolean retryResult = retryManager.attemptRetry(orderId, () -> {
                        // Simulate some work
                        try {
                            Thread.sleep(1);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    });
                    
                    // Most should succeed (unless at retry limit)
                    if (retryResult || retryManager.hasExceededRetryLimit(orderId)) {
                        // Expected outcome
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertTrue(completed, "Not all retry operations completed within timeout");
        
        long processingTime = endTime - startTime;
        double retriesPerSecond = (double) retryCount.get() / (processingTime / 1000.0);
        
        System.out.println("Retry Performance Test Results:");
        System.out.println("Total Orders: " + numberOfOrders);
        System.out.println("Total Retries: " + retryCount.get());
        System.out.println("Processing Time: " + processingTime + "ms");
        System.out.println("Retries per Second: " + String.format("%.2f", retriesPerSecond));
        
        // Verify retry performance
        assertTrue(retriesPerSecond > 100, "Retry rate should be > 100 retries/second, was: " + retriesPerSecond);
        assertTrue(retryCount.get() > 0, "Should have performed some retries");
    }
    
    @Test
    void testTimeoutSchedulingPerformance() throws Exception {
        // Given
        int numberOfTimeouts = 1000;
        CountDownLatch latch = new CountDownLatch(numberOfTimeouts);
        
        // When - Schedule many timeouts concurrently
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfTimeouts; i++) {
            final String orderId = "timeout-test-order-" + i;
            
            executorService.submit(() -> {
                try {
                    timeoutManager.scheduleCartValidationTimeout(orderId, (id, step) -> {
                        // Timeout handler
                    });
                    
                    // Immediately cancel to test cancellation performance
                    timeoutManager.cancelTimeout(orderId);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        
        // Then
        assertTrue(completed, "Not all timeout operations completed within timeout");
        
        long processingTime = endTime - startTime;
        double timeoutsPerSecond = (double) numberOfTimeouts / (processingTime / 1000.0);
        
        System.out.println("Timeout Scheduling Performance Test Results:");
        System.out.println("Total Timeouts: " + numberOfTimeouts);
        System.out.println("Processing Time: " + processingTime + "ms");
        System.out.println("Timeouts per Second: " + String.format("%.2f", timeoutsPerSecond));
        
        // Verify timeout scheduling performance
        assertTrue(timeoutsPerSecond > 200, "Timeout scheduling rate should be > 200/second, was: " + timeoutsPerSecond);
        
        // Verify scheduler interactions
        verify(taskScheduler, times(numberOfTimeouts)).schedule(any(Runnable.class), any(Instant.class));
        verify(scheduledFuture, times(numberOfTimeouts)).cancel(false);
    }
    
    @Test
    void testMemoryUsageUnderLoad() throws Exception {
        // Given
        int numberOfOrders = 10000;
        Runtime runtime = Runtime.getRuntime();
        
        // Measure initial memory
        System.gc();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        List<OrderItem> items = Arrays.asList(
            new OrderItem("item1", "Pizza", new BigDecimal("15.99"), 1)
        );
        BigDecimal totalAmount = new BigDecimal("15.99");
        
        when(sagaRepository.findByOrderId(any())).thenAnswer(invocation -> {
            String orderId = invocation.getArgument(0);
            return Optional.of(new OrderSagaData(orderId, "customer-123", "restaurant-456", items, totalAmount));
        });
        when(sagaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When - Create many saga instances
        for (int i = 0; i < numberOfOrders; i++) {
            String orderId = "memory-test-order-" + i;
            orchestrator.startOrderSaga(orderId, "customer-123", "restaurant-456", items, totalAmount);
        }
        
        // Measure memory after load
        System.gc();
        long finalMemory = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = finalMemory - initialMemory;
        
        // Then
        double memoryPerOrder = (double) memoryUsed / numberOfOrders;
        
        System.out.println("Memory Usage Test Results:");
        System.out.println("Orders Created: " + numberOfOrders);
        System.out.println("Memory Used: " + (memoryUsed / 1024 / 1024) + " MB");
        System.out.println("Memory per Order: " + String.format("%.2f", memoryPerOrder) + " bytes");
        
        // Verify reasonable memory usage (less than 1KB per order)
        assertTrue(memoryPerOrder < 1024, "Memory usage per order should be < 1KB, was: " + memoryPerOrder + " bytes");
    }
    
    void tearDown() {
        if (executorService != null) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}