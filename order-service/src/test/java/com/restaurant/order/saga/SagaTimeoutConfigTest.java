package com.restaurant.order.saga;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class SagaTimeoutConfigTest {

    @Test
    void testDefaultConfiguration() {
        // Given
        SagaTimeoutConfig config = new SagaTimeoutConfig();
        
        // Then
        assertEquals(Duration.ofMinutes(2), config.getCartValidation());
        assertEquals(Duration.ofMinutes(5), config.getPaymentProcessing());
        assertEquals(Duration.ofMinutes(1), config.getOrderConfirmation());
        assertEquals(3, config.getMaxRetryAttempts());
        assertEquals(Duration.ofSeconds(1), config.getInitialRetryDelay());
        assertEquals(2.0, config.getRetryMultiplier());
        assertEquals(Duration.ofMinutes(5), config.getMaxRetryDelay());
    }
    
    @Test
    void testRetryDelayCalculation() {
        // Given
        SagaTimeoutConfig config = new SagaTimeoutConfig();
        config.setInitialRetryDelay(Duration.ofSeconds(1));
        config.setRetryMultiplier(2.0);
        config.setMaxRetryDelay(Duration.ofSeconds(10));
        
        // When & Then
        assertEquals(Duration.ofSeconds(1), config.calculateRetryDelay(1));
        assertEquals(Duration.ofSeconds(2), config.calculateRetryDelay(2));
        assertEquals(Duration.ofSeconds(4), config.calculateRetryDelay(3));
        assertEquals(Duration.ofSeconds(8), config.calculateRetryDelay(4));
        assertEquals(Duration.ofSeconds(10), config.calculateRetryDelay(5)); // Capped at max
    }
    
    @Test
    void testRetryDelayWithZeroAttempt() {
        // Given
        SagaTimeoutConfig config = new SagaTimeoutConfig();
        config.setInitialRetryDelay(Duration.ofSeconds(2));
        
        // When & Then
        assertEquals(Duration.ofSeconds(2), config.calculateRetryDelay(0));
        assertEquals(Duration.ofSeconds(2), config.calculateRetryDelay(-1));
    }
    
    @Test
    void testConfigurationSetters() {
        // Given
        SagaTimeoutConfig config = new SagaTimeoutConfig();
        
        // When
        config.setCartValidation(Duration.ofMinutes(3));
        config.setPaymentProcessing(Duration.ofMinutes(7));
        config.setOrderConfirmation(Duration.ofMinutes(2));
        config.setMaxRetryAttempts(5);
        config.setInitialRetryDelay(Duration.ofSeconds(2));
        config.setRetryMultiplier(1.5);
        config.setMaxRetryDelay(Duration.ofMinutes(10));
        
        // Then
        assertEquals(Duration.ofMinutes(3), config.getCartValidation());
        assertEquals(Duration.ofMinutes(7), config.getPaymentProcessing());
        assertEquals(Duration.ofMinutes(2), config.getOrderConfirmation());
        assertEquals(5, config.getMaxRetryAttempts());
        assertEquals(Duration.ofSeconds(2), config.getInitialRetryDelay());
        assertEquals(1.5, config.getRetryMultiplier());
        assertEquals(Duration.ofMinutes(10), config.getMaxRetryDelay());
    }
}