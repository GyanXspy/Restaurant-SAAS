package com.restaurant.order.infrastructure.resilience;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Resilient wrapper for database operations that implements circuit breaker,
 * bulkhead, and retry patterns for database resilience.
 */
@Service
public class ResilientDatabaseService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientDatabaseService.class);
    
    /**
     * Execute database read operation with resilience patterns
     */
    @CircuitBreaker(name = "database-read", fallbackMethod = "fallbackDatabaseRead")
    @Retry(name = "database-read")
    @Bulkhead(name = "database-read", fallbackMethod = "fallbackDatabaseRead")
    public <T> T executeRead(Supplier<T> operation, String operationName) {
        logger.debug("Executing resilient database read operation: {}", operationName);
        
        try {
            return operation.get();
        } catch (Exception e) {
            logger.error("Database read operation failed: {}", operationName, e);
            throw e;
        }
    }
    
    /**
     * Execute database write operation with resilience patterns
     */
    @CircuitBreaker(name = "database-write", fallbackMethod = "fallbackDatabaseWrite")
    @Retry(name = "database-write")
    @Bulkhead(name = "database-write", fallbackMethod = "fallbackDatabaseWrite")
    public <T> T executeWrite(Supplier<T> operation, String operationName) {
        logger.debug("Executing resilient database write operation: {}", operationName);
        
        try {
            return operation.get();
        } catch (Exception e) {
            logger.error("Database write operation failed: {}", operationName, e);
            throw e;
        }
    }
    
    /**
     * Execute database operation without return value with resilience patterns
     */
    @CircuitBreaker(name = "database-write", fallbackMethod = "fallbackDatabaseOperation")
    @Retry(name = "database-write")
    @Bulkhead(name = "database-write", fallbackMethod = "fallbackDatabaseOperation")
    public void executeOperation(Runnable operation, String operationName) {
        logger.debug("Executing resilient database operation: {}", operationName);
        
        try {
            operation.run();
        } catch (Exception e) {
            logger.error("Database operation failed: {}", operationName, e);
            throw e;
        }
    }
    
    /**
     * Fallback method for database read operations
     */
    public <T> T fallbackDatabaseRead(Supplier<T> operation, String operationName, Exception ex) {
        logger.warn("Database read fallback triggered for operation: {}, reason: {}", 
                   operationName, ex.getMessage());
        
        // For read operations, we can return null or throw a specific exception
        // depending on the business requirements
        throw new DatabaseUnavailableException(
            "Database read operation temporarily unavailable: " + operationName, ex);
    }
    
    /**
     * Fallback method for database write operations
     */
    public <T> T fallbackDatabaseWrite(Supplier<T> operation, String operationName, Exception ex) {
        logger.warn("Database write fallback triggered for operation: {}, reason: {}", 
                   operationName, ex.getMessage());
        
        // For write operations, we should throw an exception as we cannot proceed
        throw new DatabaseUnavailableException(
            "Database write operation temporarily unavailable: " + operationName, ex);
    }
    
    /**
     * Fallback method for database operations without return value
     */
    public void fallbackDatabaseOperation(Runnable operation, String operationName, Exception ex) {
        logger.warn("Database operation fallback triggered for operation: {}, reason: {}", 
                   operationName, ex.getMessage());
        
        // For operations without return value, we should throw an exception
        throw new DatabaseUnavailableException(
            "Database operation temporarily unavailable: " + operationName, ex);
    }
    
    /**
     * Custom exception for database unavailability
     */
    public static class DatabaseUnavailableException extends RuntimeException {
        public DatabaseUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}