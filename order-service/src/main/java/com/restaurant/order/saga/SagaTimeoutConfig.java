package com.restaurant.order.saga;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for saga timeout and retry settings.
 */
@Component
@ConfigurationProperties(prefix = "saga.timeout")
public class SagaTimeoutConfig {
    
    private Duration cartValidation = Duration.ofMinutes(2);
    private Duration paymentProcessing = Duration.ofMinutes(5);
    private Duration orderConfirmation = Duration.ofMinutes(1);
    
    private int maxRetryAttempts = 3;
    private Duration initialRetryDelay = Duration.ofSeconds(1);
    private double retryMultiplier = 2.0;
    private Duration maxRetryDelay = Duration.ofMinutes(5);
    
    // Getters and Setters
    public Duration getCartValidation() {
        return cartValidation;
    }
    
    public void setCartValidation(Duration cartValidation) {
        this.cartValidation = cartValidation;
    }
    
    public Duration getPaymentProcessing() {
        return paymentProcessing;
    }
    
    public void setPaymentProcessing(Duration paymentProcessing) {
        this.paymentProcessing = paymentProcessing;
    }
    
    public Duration getOrderConfirmation() {
        return orderConfirmation;
    }
    
    public void setOrderConfirmation(Duration orderConfirmation) {
        this.orderConfirmation = orderConfirmation;
    }
    
    public int getMaxRetryAttempts() {
        return maxRetryAttempts;
    }
    
    public void setMaxRetryAttempts(int maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
    }
    
    public Duration getInitialRetryDelay() {
        return initialRetryDelay;
    }
    
    public void setInitialRetryDelay(Duration initialRetryDelay) {
        this.initialRetryDelay = initialRetryDelay;
    }
    
    public double getRetryMultiplier() {
        return retryMultiplier;
    }
    
    public void setRetryMultiplier(double retryMultiplier) {
        this.retryMultiplier = retryMultiplier;
    }
    
    public Duration getMaxRetryDelay() {
        return maxRetryDelay;
    }
    
    public void setMaxRetryDelay(Duration maxRetryDelay) {
        this.maxRetryDelay = maxRetryDelay;
    }
    
    /**
     * Calculates the retry delay for a given attempt using exponential backoff.
     */
    public Duration calculateRetryDelay(int attemptNumber) {
        if (attemptNumber <= 0) {
            return initialRetryDelay;
        }
        
        long delayMillis = (long) (initialRetryDelay.toMillis() * Math.pow(retryMultiplier, attemptNumber - 1));
        Duration calculatedDelay = Duration.ofMillis(delayMillis);
        
        return calculatedDelay.compareTo(maxRetryDelay) > 0 ? maxRetryDelay : calculatedDelay;
    }
}