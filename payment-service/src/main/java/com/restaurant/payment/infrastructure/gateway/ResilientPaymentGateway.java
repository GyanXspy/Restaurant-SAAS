package com.restaurant.payment.infrastructure.gateway;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Resilient wrapper for PaymentGateway that implements circuit breaker,
 * bulkhead, retry, and timeout patterns for external payment processing.
 */
@Component
@Qualifier("resilientPaymentGateway")
public class ResilientPaymentGateway implements PaymentGateway {
    
    private static final Logger logger = LoggerFactory.getLogger(ResilientPaymentGateway.class);
    
    private final PaymentGateway delegate;
    
    @Autowired
    public ResilientPaymentGateway(@Qualifier("mockPaymentGateway") PaymentGateway delegate) {
        this.delegate = delegate;
    }
    
    @Override
    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "fallbackProcessPayment")
    @Retry(name = "payment-gateway")
    @Bulkhead(name = "payment-gateway", fallbackMethod = "fallbackProcessPayment")
    @TimeLimiter(name = "payment-gateway")
    public PaymentResult processPayment(PaymentRequest request) {
        logger.info("Processing payment through resilient gateway for paymentId: {}", request.getPaymentId());
        
        try {
            return delegate.processPayment(request);
        } catch (Exception e) {
            logger.error("Error processing payment for paymentId: {}", request.getPaymentId(), e);
            throw e;
        }
    }
    
    /**
     * Asynchronous version for TimeLimiter support
     */
    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "fallbackProcessPaymentAsync")
    @Retry(name = "payment-gateway")
    @Bulkhead(name = "payment-gateway", fallbackMethod = "fallbackProcessPaymentAsync")
    @TimeLimiter(name = "payment-gateway")
    public CompletionStage<PaymentResult> processPaymentAsync(PaymentRequest request) {
        logger.info("Processing payment asynchronously through resilient gateway for paymentId: {}", request.getPaymentId());
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                return delegate.processPayment(request);
            } catch (Exception e) {
                logger.error("Error processing payment asynchronously for paymentId: {}", request.getPaymentId(), e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Fallback method for synchronous payment processing
     */
    public PaymentResult fallbackProcessPayment(PaymentRequest request, Exception ex) {
        logger.warn("Payment gateway fallback triggered for paymentId: {}, reason: {}", 
                   request.getPaymentId(), ex.getMessage());
        
        // Return a failure result indicating service unavailability
        return PaymentResult.failure(
            "SERVICE_UNAVAILABLE", 
            "Payment service is temporarily unavailable. Please try again later.",
            "{\"status\":\"SERVICE_UNAVAILABLE\",\"fallback\":true,\"timestamp\":" + System.currentTimeMillis() + "}"
        );
    }
    
    /**
     * Fallback method for asynchronous payment processing
     */
    public CompletionStage<PaymentResult> fallbackProcessPaymentAsync(PaymentRequest request, Exception ex) {
        logger.warn("Payment gateway async fallback triggered for paymentId: {}, reason: {}", 
                   request.getPaymentId(), ex.getMessage());
        
        PaymentResult fallbackResult = PaymentResult.failure(
            "SERVICE_UNAVAILABLE", 
            "Payment service is temporarily unavailable. Please try again later.",
            "{\"status\":\"SERVICE_UNAVAILABLE\",\"fallback\":true,\"async\":true,\"timestamp\":" + System.currentTimeMillis() + "}"
        );
        
        return CompletableFuture.completedFuture(fallbackResult);
    }
}