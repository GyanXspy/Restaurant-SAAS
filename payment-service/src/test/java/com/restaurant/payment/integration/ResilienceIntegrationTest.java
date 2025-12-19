package com.restaurant.payment.integration;

import com.restaurant.payment.domain.PaymentMethod;
import com.restaurant.payment.infrastructure.gateway.PaymentGateway;
import com.restaurant.payment.infrastructure.gateway.ResilientPaymentGateway;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.payment-gateway.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.payment-gateway.wait-duration-in-open-state=2s",
    "resilience4j.circuitbreaker.instances.payment-gateway.sliding-window-size=6",
    "resilience4j.circuitbreaker.instances.payment-gateway.minimum-number-of-calls=3",
    "resilience4j.bulkhead.instances.payment-gateway.max-concurrent-calls=5",
    "resilience4j.bulkhead.instances.payment-gateway.max-wait-duration=1s",
    "resilience4j.retry.instances.payment-gateway.max-attempts=3",
    "resilience4j.retry.instances.payment-gateway.wait-duration=500ms"
})
class ResilienceIntegrationTest {

    @Autowired
    private ResilientPaymentGateway resilientGateway;

    @MockBean(name = "mockPaymentGateway")
    private PaymentGateway mockDelegate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private PaymentGateway.PaymentRequest testRequest;

    @BeforeEach
    void setUp() {
        testRequest = new PaymentGateway.PaymentRequest(
            "payment-integration-test",
            new BigDecimal("100.00"),
            PaymentMethod.CREDIT_CARD,
            "4111111111111111",
            "customer-integration"
        );

        // Reset circuit breaker state
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment-gateway");
        circuitBreaker.transitionToClosedState();
    }

    @Test
    void shouldHandleHighConcurrencyWithBulkhead() throws InterruptedException {
        // Given
        when(mockDelegate.processPayment(any()))
            .thenReturn(PaymentGateway.PaymentResult.success("TXN-BULK", "{\"status\":\"SUCCESS\"}"));

        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // When - Submit more requests than bulkhead allows
        for (int i = 0; i < 15; i++) {
            final int requestId = i;
            executor.submit(() -> {
                try {
                    PaymentGateway.PaymentRequest request = new PaymentGateway.PaymentRequest(
                        "payment-bulk-" + requestId,
                        new BigDecimal("50.00"),
                        PaymentMethod.CREDIT_CARD,
                        "4111111111111111",
                        "customer-bulk-" + requestId
                    );
                    
                    PaymentGateway.PaymentResult result = resilientGateway.processPayment(request);
                    if (result.isSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        // Then - Some requests should succeed, some might be rejected by bulkhead
        assertTrue(successCount.get() > 0, "At least some requests should succeed");
        int totalProcessed = successCount.get() + failureCount.get();
        assertEquals(15, totalProcessed, "All requests should be processed (success or failure)");
    }

    @Test
    void shouldTriggerCircuitBreakerAfterFailures() {
        // Given - Configure mock to fail consistently
        when(mockDelegate.processPayment(any()))
            .thenThrow(new RuntimeException("Simulated gateway failure"));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment-gateway");
        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());

        // When - Make enough failing calls to trigger circuit breaker
        for (int i = 0; i < 5; i++) {
            try {
                resilientGateway.processPayment(testRequest);
            } catch (Exception e) {
                // Expected failures
            }
        }

        // Then - Circuit breaker should be open
        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // When - Try another call with circuit breaker open
        Exception exception = assertThrows(Exception.class, () -> {
            resilientGateway.processPayment(testRequest);
        });

        // Then - Should get circuit breaker exception or fallback
        assertTrue(exception.getMessage().contains("SERVICE_UNAVAILABLE") || 
                  exception.getClass().getSimpleName().contains("CallNotPermitted"));
    }

    @Test
    void shouldRecoverAfterCircuitBreakerTimeout() throws InterruptedException {
        // Given - Trigger circuit breaker to open
        when(mockDelegate.processPayment(any()))
            .thenThrow(new RuntimeException("Initial failures"));

        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("payment-gateway");

        // Trigger failures to open circuit breaker
        for (int i = 0; i < 5; i++) {
            try {
                resilientGateway.processPayment(testRequest);
            } catch (Exception e) {
                // Expected failures
            }
        }

        assertEquals(CircuitBreaker.State.OPEN, circuitBreaker.getState());

        // When - Wait for circuit breaker to transition to half-open
        Thread.sleep(2500); // Wait longer than wait-duration-in-open-state (2s)

        // Configure mock to succeed now
        when(mockDelegate.processPayment(any()))
            .thenReturn(PaymentGateway.PaymentResult.success("TXN-RECOVERY", "{\"status\":\"SUCCESS\"}"));

        // Then - Circuit breaker should allow calls and eventually close
        PaymentGateway.PaymentResult result = resilientGateway.processPayment(testRequest);
        assertNotNull(result);
        
        // Make a few more successful calls to close the circuit breaker
        for (int i = 0; i < 3; i++) {
            resilientGateway.processPayment(testRequest);
        }

        assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
    }

    @Test
    void shouldRetryTransientFailuresBeforeFallback() {
        // Given - Configure mock to fail twice then succeed
        AtomicInteger attemptCount = new AtomicInteger(0);
        when(mockDelegate.processPayment(any())).thenAnswer(invocation -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt <= 2) {
                throw new RuntimeException("Transient failure attempt " + attempt);
            }
            return PaymentGateway.PaymentResult.success("TXN-RETRY", "{\"status\":\"SUCCESS\"}");
        });

        // When
        PaymentGateway.PaymentResult result = resilientGateway.processPayment(testRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("TXN-RETRY", result.getTransactionId());
        assertEquals(3, attemptCount.get()); // Should have retried 3 times total
    }

    @Test
    void shouldHandleAsyncOperationsWithResilience() {
        // Given
        when(mockDelegate.processPayment(any()))
            .thenReturn(PaymentGateway.PaymentResult.success("TXN-ASYNC", "{\"status\":\"SUCCESS\"}"));

        // When
        CompletableFuture<PaymentGateway.PaymentResult> future = 
            resilientGateway.processPaymentAsync(testRequest).toCompletableFuture();

        // Then
        PaymentGateway.PaymentResult result = future.join();
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("TXN-ASYNC", result.getTransactionId());
    }

    @Test
    void shouldProvideConsistentFallbackBehavior() {
        // Given
        when(mockDelegate.processPayment(any()))
            .thenThrow(new RuntimeException("Persistent failure for fallback test"));

        // When & Then - Synchronous fallback
        Exception syncException = assertThrows(Exception.class, () -> {
            resilientGateway.processPayment(testRequest);
        });

        // When & Then - Asynchronous fallback
        Exception asyncException = assertThrows(Exception.class, () -> {
            resilientGateway.processPaymentAsync(testRequest).toCompletableFuture().join();
        });

        // Both should result in consistent fallback behavior
        assertTrue(syncException.getMessage().contains("SERVICE_UNAVAILABLE") || 
                  syncException.getCause() != null);
        assertTrue(asyncException.getMessage().contains("SERVICE_UNAVAILABLE") || 
                  asyncException.getCause() != null);
    }

    @Test
    void shouldMaintainPerformanceUnderNormalConditions() {
        // Given
        when(mockDelegate.processPayment(any()))
            .thenReturn(PaymentGateway.PaymentResult.success("TXN-PERF", "{\"status\":\"SUCCESS\"}"));

        // When - Measure performance of multiple calls
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < 10; i++) {
            PaymentGateway.PaymentResult result = resilientGateway.processPayment(testRequest);
            assertTrue(result.isSuccess());
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Then - Should complete reasonably quickly (allowing for some overhead)
        assertTrue(totalTime < 5000, "10 calls should complete within 5 seconds under normal conditions");
    }
}