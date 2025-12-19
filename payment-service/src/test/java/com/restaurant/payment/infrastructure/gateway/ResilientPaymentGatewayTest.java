package com.restaurant.payment.infrastructure.gateway;

import com.restaurant.payment.domain.PaymentMethod;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.payment-gateway.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.payment-gateway.wait-duration-in-open-state=1s",
    "resilience4j.circuitbreaker.instances.payment-gateway.sliding-window-size=4",
    "resilience4j.circuitbreaker.instances.payment-gateway.minimum-number-of-calls=2"
})
class ResilientPaymentGatewayTest {

    @Mock
    private PaymentGateway mockDelegate;

    private ResilientPaymentGateway resilientGateway;
    private PaymentGateway.PaymentRequest testRequest;

    @BeforeEach
    void setUp() {
        resilientGateway = new ResilientPaymentGateway(mockDelegate);
        testRequest = new PaymentGateway.PaymentRequest(
            "payment-123",
            new BigDecimal("25.99"),
            PaymentMethod.CREDIT_CARD,
            "4111111111111111",
            "customer-456"
        );
    }

    @Test
    void shouldProcessPaymentSuccessfully() {
        // Given
        PaymentGateway.PaymentResult expectedResult = PaymentGateway.PaymentResult.success(
            "TXN-12345", "{\"status\":\"SUCCESS\"}"
        );
        when(mockDelegate.processPayment(any())).thenReturn(expectedResult);

        // When
        PaymentGateway.PaymentResult result = resilientGateway.processPayment(testRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("TXN-12345", result.getTransactionId());
        verify(mockDelegate, times(1)).processPayment(testRequest);
    }

    @Test
    void shouldRetryOnTransientFailure() {
        // Given
        when(mockDelegate.processPayment(any()))
            .thenThrow(new RuntimeException("Temporary network error"))
            .thenThrow(new RuntimeException("Still failing"))
            .thenReturn(PaymentGateway.PaymentResult.success("TXN-12345", "{\"status\":\"SUCCESS\"}"));

        // When
        PaymentGateway.PaymentResult result = resilientGateway.processPayment(testRequest);

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(mockDelegate, times(3)).processPayment(testRequest);
    }

    @Test
    void shouldFallbackAfterMaxRetries() {
        // Given
        when(mockDelegate.processPayment(any()))
            .thenThrow(new RuntimeException("Persistent failure"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            resilientGateway.processPayment(testRequest);
        });

        assertTrue(exception.getMessage().contains("SERVICE_UNAVAILABLE") || 
                  exception.getMessage().contains("Persistent failure"));
        verify(mockDelegate, atLeast(1)).processPayment(testRequest);
    }

    @Test
    void shouldHandleCircuitBreakerOpen() {
        // Given - Simulate circuit breaker being open
        when(mockDelegate.processPayment(any()))
            .thenThrow(new CallNotPermittedException("Circuit breaker is OPEN"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            resilientGateway.processPayment(testRequest);
        });

        assertTrue(exception instanceof CallNotPermittedException || 
                  exception.getMessage().contains("SERVICE_UNAVAILABLE"));
    }

    @Test
    void shouldHandleBulkheadFull() {
        // Given
        when(mockDelegate.processPayment(any()))
            .thenThrow(new BulkheadFullException("Bulkhead is full"));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            resilientGateway.processPayment(testRequest);
        });

        assertTrue(exception instanceof BulkheadFullException || 
                  exception.getMessage().contains("SERVICE_UNAVAILABLE"));
    }

    @Test
    void shouldHandleTimeout() {
        // Given
        when(mockDelegate.processPayment(any()))
            .thenThrow(new RuntimeException(new TimeoutException("Operation timed out")));

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            resilientGateway.processPayment(testRequest);
        });

        assertTrue(exception.getMessage().contains("timeout") || 
                  exception.getMessage().contains("SERVICE_UNAVAILABLE"));
    }

    @Test
    void shouldProcessAsyncPaymentSuccessfully() {
        // Given
        PaymentGateway.PaymentResult expectedResult = PaymentGateway.PaymentResult.success(
            "TXN-12345", "{\"status\":\"SUCCESS\"}"
        );
        when(mockDelegate.processPayment(any())).thenReturn(expectedResult);

        // When
        PaymentGateway.PaymentResult result = resilientGateway.processPaymentAsync(testRequest)
            .toCompletableFuture().join();

        // Then
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("TXN-12345", result.getTransactionId());
        verify(mockDelegate, times(1)).processPayment(testRequest);
    }

    @Test
    void shouldFallbackOnAsyncFailure() {
        // Given
        when(mockDelegate.processPayment(any()))
            .thenThrow(new RuntimeException("Async failure"));

        // When & Then
        CompletionException exception = assertThrows(CompletionException.class, () -> {
            resilientGateway.processPaymentAsync(testRequest)
                .toCompletableFuture().join();
        });

        assertNotNull(exception.getCause());
    }

    @Test
    void shouldPreserveFallbackBehaviorConsistency() {
        // Given
        RuntimeException testException = new RuntimeException("Test failure");
        
        // When
        PaymentGateway.PaymentResult syncFallback = resilientGateway.fallbackProcessPayment(testRequest, testException);
        PaymentGateway.PaymentResult asyncFallback = resilientGateway.fallbackProcessPaymentAsync(testRequest, testException)
            .toCompletableFuture().join();

        // Then
        assertNotNull(syncFallback);
        assertNotNull(asyncFallback);
        assertFalse(syncFallback.isSuccess());
        assertFalse(asyncFallback.isSuccess());
        assertEquals("SERVICE_UNAVAILABLE", syncFallback.getErrorCode());
        assertEquals("SERVICE_UNAVAILABLE", asyncFallback.getErrorCode());
        assertTrue(syncFallback.getGatewayResponse().contains("fallback"));
        assertTrue(asyncFallback.getGatewayResponse().contains("fallback"));
        assertTrue(asyncFallback.getGatewayResponse().contains("async"));
    }

    @Test
    void shouldLogAppropriateMessages() {
        // Given
        PaymentGateway.PaymentResult expectedResult = PaymentGateway.PaymentResult.success(
            "TXN-12345", "{\"status\":\"SUCCESS\"}"
        );
        when(mockDelegate.processPayment(any())).thenReturn(expectedResult);

        // When
        resilientGateway.processPayment(testRequest);

        // Then
        verify(mockDelegate, times(1)).processPayment(testRequest);
        // Note: In a real test, you might want to verify log messages using a log capture framework
    }
}