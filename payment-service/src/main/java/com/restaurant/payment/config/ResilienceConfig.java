package com.restaurant.payment.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j patterns including circuit breaker,
 * bulkhead, retry, and time limiter for payment gateway resilience.
 */
@Configuration
public class ResilienceConfig {
    
    /**
     * Circuit breaker configuration for payment gateway
     */
    @Bean
    public CircuitBreaker paymentGatewayCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // Open circuit if 50% of calls fail
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
            .slidingWindowSize(10) // Consider last 10 calls
            .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
            .slowCallRateThreshold(50) // Consider slow calls as failures
            .slowCallDurationThreshold(Duration.ofSeconds(5)) // Calls taking >5s are slow
            .build();
        
        return CircuitBreaker.of("payment-gateway", config);
    }
    
    /**
     * Bulkhead configuration for payment gateway to isolate resources
     */
    @Bean
    public Bulkhead paymentGatewayBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(10) // Allow max 10 concurrent payment calls
            .maxWaitDuration(Duration.ofSeconds(2)) // Wait max 2s for available slot
            .build();
        
        return Bulkhead.of("payment-gateway", config);
    }
    
    /**
     * Retry configuration for payment gateway
     */
    @Bean
    public Retry paymentGatewayRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3) // Retry up to 3 times
            .waitDuration(Duration.ofSeconds(1)) // Wait 1s between retries
            .exponentialBackoffMultiplier(2) // Exponential backoff: 1s, 2s, 4s
            .retryOnException(throwable -> {
                // Retry on specific exceptions
                return throwable instanceof RuntimeException ||
                       throwable instanceof java.util.concurrent.TimeoutException ||
                       throwable.getMessage().contains("timeout") ||
                       throwable.getMessage().contains("connection");
            })
            .build();
        
        return Retry.of("payment-gateway", config);
    }
    
    /**
     * Time limiter configuration for payment gateway
     */
    @Bean
    public TimeLimiter paymentGatewayTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10)) // Timeout after 10 seconds
            .cancelRunningFuture(true) // Cancel the running future on timeout
            .build();
        
        return TimeLimiter.of("payment-gateway", config);
    }
}