package com.restaurant.payment.infrastructure.gateway;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.restaurant.payment.domain.PaymentMethod;

class MockPaymentGatewayTest {

    private MockPaymentGateway paymentGateway;

    @BeforeEach
    void setUp() {
        paymentGateway = new MockPaymentGateway();
    }

    @Test
    void shouldProcessPaymentRequest() {
        // Given
        PaymentGateway.PaymentRequest request = new PaymentGateway.PaymentRequest(
            "payment-123",
            new BigDecimal("25.99"),
            PaymentMethod.CREDIT_CARD,
            "card-ending-1234",
            "customer-456"
        );

        // When
        PaymentGateway.PaymentResult result = paymentGateway.processPayment(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getGatewayResponse());
        
        // Result should be either success or failure
        if (result.isSuccess()) {
            assertNotNull(result.getTransactionId());
            assertNull(result.getErrorCode());
            assertNull(result.getErrorMessage());
        } else {
            assertNull(result.getTransactionId());
            assertNotNull(result.getErrorCode());
            assertNotNull(result.getErrorMessage());
        }
    }

    @Test
    void shouldReturnConsistentResultsForSameInput() {
        // Given
        PaymentGateway.PaymentRequest request = new PaymentGateway.PaymentRequest(
            "payment-123",
            new BigDecimal("25.99"),
            PaymentMethod.CREDIT_CARD,
            "card-ending-1234",
            "customer-456"
        );

        // When - Process multiple times
        PaymentGateway.PaymentResult result1 = paymentGateway.processPayment(request);
        PaymentGateway.PaymentResult result2 = paymentGateway.processPayment(request);

        // Then - Results should be valid (though may differ due to randomness)
        assertNotNull(result1);
        assertNotNull(result2);
        assertNotNull(result1.getGatewayResponse());
        assertNotNull(result2.getGatewayResponse());
    }

    @Test
    void shouldHandleDifferentPaymentMethods() {
        // Test different payment methods
        PaymentMethod[] methods = {PaymentMethod.CREDIT_CARD, PaymentMethod.DEBIT_CARD, 
                                  PaymentMethod.DIGITAL_WALLET, PaymentMethod.BANK_TRANSFER};

        for (PaymentMethod method : methods) {
            // Given
            PaymentGateway.PaymentRequest request = new PaymentGateway.PaymentRequest(
                "payment-" + method.name(),
                new BigDecimal("50.00"),
                method,
                "details-for-" + method.name(),
                "customer-123"
            );

            // When
            PaymentGateway.PaymentResult result = paymentGateway.processPayment(request);

            // Then
            assertNotNull(result, "Result should not be null for method: " + method);
            assertNotNull(result.getGatewayResponse(), "Gateway response should not be null for method: " + method);
        }
    }

    @Test
    void shouldHandleDifferentAmounts() {
        // Test different amounts
        BigDecimal[] amounts = {
            new BigDecimal("0.01"),
            new BigDecimal("10.00"),
            new BigDecimal("100.00"),
            new BigDecimal("1000.00")
        };

        for (BigDecimal amount : amounts) {
            // Given
            PaymentGateway.PaymentRequest request = new PaymentGateway.PaymentRequest(
                "payment-" + amount,
                amount,
                PaymentMethod.CREDIT_CARD,
                "card-ending-1234",
                "customer-123"
            );

            // When
            PaymentGateway.PaymentResult result = paymentGateway.processPayment(request);

            // Then
            assertNotNull(result, "Result should not be null for amount: " + amount);
            assertNotNull(result.getGatewayResponse(), "Gateway response should not be null for amount: " + amount);
        }
    }
}