package com.restaurant.payment.infrastructure.gateway;

import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("mockPaymentGateway")
public class MockPaymentGateway implements PaymentGateway {
    
    private static final Logger logger = LoggerFactory.getLogger(MockPaymentGateway.class);
    private final Random random = new Random();
    
    // Simulate different failure scenarios
    private static final double SUCCESS_RATE = 0.85; // 85% success rate
    private static final double TIMEOUT_RATE = 0.05; // 5% timeout rate
    private static final double INSUFFICIENT_FUNDS_RATE = 0.07; // 7% insufficient funds
    // Remaining 3% for other failures

    @Override
    public PaymentResult processPayment(PaymentRequest request) {
        logger.info("Processing payment for paymentId: {}, amount: {}, method: {}", 
                   request.getPaymentId(), request.getAmount(), request.getPaymentMethod());
        
        // Simulate processing delay
        try {
            Thread.sleep(random.nextInt(1000) + 500); // 500-1500ms delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentResult.failure("PROCESSING_INTERRUPTED", "Payment processing was interrupted", "INTERRUPTED");
        }
        
        // Simulate different outcomes based on probability
        double outcome = random.nextDouble();
        
        if (outcome < SUCCESS_RATE) {
            return processSuccessfulPayment(request);
        } else if (outcome < SUCCESS_RATE + TIMEOUT_RATE) {
            return processTimeoutFailure(request);
        } else if (outcome < SUCCESS_RATE + TIMEOUT_RATE + INSUFFICIENT_FUNDS_RATE) {
            return processInsufficientFundsFailure(request);
        } else {
            return processGenericFailure(request);
        }
    }
    
    private PaymentResult processSuccessfulPayment(PaymentRequest request) {
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String gatewayResponse = String.format(
            "{\"status\":\"SUCCESS\",\"transactionId\":\"%s\",\"amount\":%s,\"method\":\"%s\",\"timestamp\":\"%s\"}", 
            transactionId, request.getAmount(), request.getPaymentMethod(), System.currentTimeMillis());
        
        logger.info("Payment successful for paymentId: {}, transactionId: {}", 
                   request.getPaymentId(), transactionId);
        
        return PaymentResult.success(transactionId, gatewayResponse);
    }
    
    private PaymentResult processTimeoutFailure(PaymentRequest request) {
        String gatewayResponse = String.format(
            "{\"status\":\"TIMEOUT\",\"error\":\"Gateway timeout\",\"amount\":%s,\"timestamp\":\"%s\"}", 
            request.getAmount(), System.currentTimeMillis());
        
        logger.warn("Payment timeout for paymentId: {}", request.getPaymentId());
        
        return PaymentResult.failure("GATEWAY_TIMEOUT", "Payment gateway timeout", gatewayResponse);
    }
    
    private PaymentResult processInsufficientFundsFailure(PaymentRequest request) {
        String gatewayResponse = String.format(
            "{\"status\":\"DECLINED\",\"error\":\"Insufficient funds\",\"amount\":%s,\"timestamp\":\"%s\"}", 
            request.getAmount(), System.currentTimeMillis());
        
        logger.warn("Payment declined due to insufficient funds for paymentId: {}", request.getPaymentId());
        
        return PaymentResult.failure("INSUFFICIENT_FUNDS", "Insufficient funds available", gatewayResponse);
    }
    
    private PaymentResult processGenericFailure(PaymentRequest request) {
        String[] errorCodes = {"INVALID_CARD", "EXPIRED_CARD", "CARD_BLOCKED", "NETWORK_ERROR"};
        String[] errorMessages = {
            "Invalid card number", 
            "Card has expired", 
            "Card is blocked", 
            "Network communication error"
        };
        
        int errorIndex = random.nextInt(errorCodes.length);
        String errorCode = errorCodes[errorIndex];
        String errorMessage = errorMessages[errorIndex];
        
        String gatewayResponse = String.format(
            "{\"status\":\"FAILED\",\"error\":\"%s\",\"errorCode\":\"%s\",\"amount\":%s,\"timestamp\":\"%s\"}", 
            errorMessage, errorCode, request.getAmount(), System.currentTimeMillis());
        
        logger.warn("Payment failed for paymentId: {}, error: {}", request.getPaymentId(), errorMessage);
        
        return PaymentResult.failure(errorCode, errorMessage, gatewayResponse);
    }
}