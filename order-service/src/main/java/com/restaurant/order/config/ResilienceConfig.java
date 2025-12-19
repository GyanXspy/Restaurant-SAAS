package com.restaurant.order.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j patterns for order service operations
 * including database operations and event publishing.
 */
@Configuration
public class ResilienceConfig {
    
    /**
     * Circuit breaker configuration for database read operations
     */
    @Bean
    public CircuitBreaker databaseReadCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(60) // Open circuit if 60% of calls fail
            .waitDurationInOpenState(Duration.ofSeconds(20)) // Wait 20s before trying again
            .slidingWindowSize(8) // Consider last 8 calls
            .minimumNumberOfCalls(4) // Need at least 4 calls before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(2) // Allow 2 calls in half-open state
            .slowCallRateThreshold(70) // Consider slow calls as failures
            .slowCallDurationThreshold(Duration.ofSeconds(3)) // Calls taking >3s are slow
            .build();
        
        return CircuitBreaker.of("database-read", config);
    }
    
    /**
     * Circuit breaker configuration for database write operations
     */
    @Bean
    public CircuitBreaker databaseWriteCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(40) // More strict for write operations
            .waitDurationInOpenState(Duration.ofSeconds(30)) // Wait 30s before trying again
            .slidingWindowSize(10) // Consider last 10 calls
            .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(2) // Allow 2 calls in half-open state
            .slowCallRateThreshold(50) // Consider slow calls as failures
            .slowCallDurationThreshold(Duration.ofSeconds(5)) // Calls taking >5s are slow
            .build();
        
        return CircuitBreaker.of("database-write", config);
    }
    
    /**
     * Circuit breaker configuration for event publishing
     */
    @Bean
    public CircuitBreaker eventPublisherCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50) // Open circuit if 50% of calls fail
            .waitDurationInOpenState(Duration.ofSeconds(15)) // Wait 15s before trying again
            .slidingWindowSize(10) // Consider last 10 calls
            .minimumNumberOfCalls(5) // Need at least 5 calls before calculating failure rate
            .permittedNumberOfCallsInHalfOpenState(3) // Allow 3 calls in half-open state
            .slowCallRateThreshold(60) // Consider slow calls as failures
            .slowCallDurationThreshold(Duration.ofSeconds(2)) // Calls taking >2s are slow
            .build();
        
        return CircuitBreaker.of("event-publisher", config);
    }
    
    /**
     * Bulkhead configuration for database read operations
     */
    @Bean
    public Bulkhead databaseReadBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(15) // Allow max 15 concurrent read calls
            .maxWaitDuration(Duration.ofSeconds(1)) // Wait max 1s for available slot
            .build();
        
        return Bulkhead.of("database-read", config);
    }
    
    /**
     * Bulkhead configuration for database write operations
     */
    @Bean
    public Bulkhead databaseWriteBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(8) // Allow max 8 concurrent write calls
            .maxWaitDuration(Duration.ofSeconds(2)) // Wait max 2s for available slot
            .build();
        
        return Bulkhead.of("database-write", config);
    }
    
    /**
     * Bulkhead configuration for event publishing
     */
    @Bean
    public Bulkhead eventPublisherBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
            .maxConcurrentCalls(20) // Allow max 20 concurrent event publishing calls
            .maxWaitDuration(Duration.ofSeconds(1)) // Wait max 1s for available slot
            .build();
        
        return Bulkhead.of("event-publisher", config);
    }
    
    /**
     * Retry configuration for database read operations
     */
    @Bean
    public Retry databaseReadRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3) // Retry up to 3 times
            .waitDuration(Duration.ofMillis(500)) // Wait 500ms between retries
            .exponentialBackoffMultiplier(1.5) // Exponential backoff: 500ms, 750ms, 1125ms
            .retryOnException(throwable -> {
                // Retry on specific database exceptions
                return throwable instanceof java.sql.SQLException ||
                       throwable instanceof org.springframework.dao.DataAccessException ||
                       throwable.getMessage().contains("connection") ||
                       throwable.getMessage().contains("timeout");
            })
            .build();
        
        return Retry.of("database-read", config);
    }
    
    /**
     * Retry configuration for database write operations
     */
    @Bean
    public Retry databaseWriteRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(2) // Retry up to 2 times for write operations
            .waitDuration(Duration.ofSeconds(1)) // Wait 1s between retries
            .exponentialBackoffMultiplier(2) // Exponential backoff: 1s, 2s
            .retryOnException(throwable -> {
                // Retry on specific database exceptions, but be careful with writes
                return throwable instanceof java.sql.SQLTransientException ||
                       (throwable instanceof java.sql.SQLException && 
                        throwable.getMessage().contains("connection")) ||
                       throwable.getMessage().contains("timeout");
            })
            .build();
        
        return Retry.of("database-write", config);
    }
    
    /**
     * Retry configuration for event publishing
     */
    @Bean
    public Retry eventPublisherRetry() {
        RetryConfig config = RetryConfig.custom()
            .maxAttempts(3) // Retry up to 3 times
            .waitDuration(Duration.ofMillis(800)) // Wait 800ms between retries
            .exponentialBackoffMultiplier(1.8) // Exponential backoff: 800ms, 1440ms, 2592ms
            .retryOnException(throwable -> {
                // Retry on specific Kafka exceptions
                return throwable instanceof org.apache.kafka.common.errors.RetriableException ||
                       throwable instanceof org.springframework.kafka.KafkaException ||
                       throwable.getMessage().contains("timeout") ||
                       throwable.getMessage().contains("connection") ||
                       throwable.getMessage().contains("network");
            })
            .build();
        
        return Retry.of("event-publisher", config);
    }
}