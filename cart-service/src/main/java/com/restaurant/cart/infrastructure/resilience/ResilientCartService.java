package com.restaurant.cart.infrastructure.resilience;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Resilient wrapper for cart operations that implements circuit breaker,
 * bulkhead, and retry patterns for MongoDB operations and external service calls.
 */
@Service
public class ResilientCartService {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientCartService.class);
    
    /**
     * Execute MongoDB operation with resilience patterns
     */
    @CircuitBreaker(name = "mongodb", fallbackMethod = "fallbackMongoOperation")
    @Retry(name = "mongodb")
    @Bulkhead(name = "mongodb", fallbackMethod = "fallbackMongoOperation")
    public <T> T executeMongoOperation(Supplier<T> operation, String operationName) {
        logger.debug("Executing resilient MongoDB operation: {}", operationName);
        
        try {
            return operation.get();
        } catch (Exception e) {
            logger.error("MongoDB operation failed: {}", operationName, e);
            throw e;
        }
    }
    
    /**
     * Execute MongoDB operation without return value with resilience patterns
     */
    @CircuitBreaker(name = "mongodb", fallbackMethod = "fallbackMongoVoidOperation")
    @Retry(name = "mongodb")
    @Bulkhead(name = "mongodb", fallbackMethod = "fallbackMongoVoidOperation")
    public void executeMongoVoidOperation(Runnable operation, String operationName) {
        logger.debug("Executing resilient MongoDB void operation: {}", operationName);
        
        try {
            operation.run();
        } catch (Exception e) {
            logger.error("MongoDB void operation failed: {}", operationName, e);
            throw e;
        }
    }
    
    /**
     * Execute external service call with resilience patterns
     * (e.g., restaurant service for menu validation)
     */
    @CircuitBreaker(name = "external-service", fallbackMethod = "fallbackExternalService")
    @Retry(name = "external-service")
    @Bulkhead(name = "external-service", fallbackMethod = "fallbackExternalService")
    public <T> T executeExternalServiceCall(Supplier<T> operation, String serviceName) {
        logger.debug("Executing resilient external service call: {}", serviceName);
        
        try {
            return operation.get();
        } catch (Exception e) {
            logger.error("External service call failed: {}", serviceName, e);
            throw e;
        }
    }
    
    /**
     * Fallback method for MongoDB operations
     */
    public <T> T fallbackMongoOperation(Supplier<T> operation, String operationName, Exception ex) {
        logger.warn("MongoDB operation fallback triggered for operation: {}, reason: {}", 
                   operationName, ex.getMessage());
        
        // For MongoDB operations, we can return null or throw a specific exception
        throw new MongoUnavailableException(
            "MongoDB operation temporarily unavailable: " + operationName, ex);
    }
    
    /**
     * Fallback method for MongoDB void operations
     */
    public void fallbackMongoVoidOperation(Runnable operation, String operationName, Exception ex) {
        logger.warn("MongoDB void operation fallback triggered for operation: {}, reason: {}", 
                   operationName, ex.getMessage());
        
        // For void operations, we should throw an exception
        throw new MongoUnavailableException(
            "MongoDB operation temporarily unavailable: " + operationName, ex);
    }
    
    /**
     * Fallback method for external service calls
     */
    public <T> T fallbackExternalService(Supplier<T> operation, String serviceName, Exception ex) {
        logger.warn("External service fallback triggered for service: {}, reason: {}", 
                   serviceName, ex.getMessage());
        
        // For external service calls, we might return cached data or throw exception
        throw new ExternalServiceUnavailableException(
            "External service temporarily unavailable: " + serviceName, ex);
    }
    
    /**
     * Custom exception for MongoDB unavailability
     */
    public static class MongoUnavailableException extends RuntimeException {
        public MongoUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Custom exception for external service unavailability
     */
    public static class ExternalServiceUnavailableException extends RuntimeException {
        public ExternalServiceUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}