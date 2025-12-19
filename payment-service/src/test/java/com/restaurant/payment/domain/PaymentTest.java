package com.restaurant.payment.domain;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.restaurant.events.DomainEvent;
import com.restaurant.payment.domain.events.PaymentCompletedEvent;
import com.restaurant.payment.domain.events.PaymentFailedEvent;
import com.restaurant.payment.domain.events.PaymentInitiatedEvent;

class PaymentTest {

    @Test
    void shouldCreatePaymentWithInitiatedEvent() {
        // Given
        String orderId = "order-123";
        String customerId = "customer-456";
        BigDecimal amount = new BigDecimal("25.99");
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
        String paymentDetails = "card-ending-1234";

        // When
        Payment payment = new Payment(orderId, customerId, amount, paymentMethod, paymentDetails);

        // Then
        assertNotNull(payment.getPaymentId());
        assertEquals(orderId, payment.getOrderId());
        assertEquals(customerId, payment.getCustomerId());
        assertEquals(amount, payment.getAmount());
        assertEquals(paymentMethod, payment.getPaymentMethod());
        assertEquals(paymentDetails, payment.getPaymentDetails());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());
        assertTrue(payment.isValidForProcessing());

        List<DomainEvent> events = payment.getUncommittedEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof PaymentInitiatedEvent);
    }

    @Test
    void shouldCompletePaymentSuccessfully() {
        // Given
        Payment payment = createTestPayment();
        String transactionId = "txn-789";
        String gatewayResponse = "SUCCESS";

        // When
        payment.completePayment(transactionId, gatewayResponse);

        // Then
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(transactionId, payment.getTransactionId());

        List<DomainEvent> events = payment.getUncommittedEvents();
        assertEquals(2, events.size());
        assertTrue(events.get(1) instanceof PaymentCompletedEvent);
    }

    @Test
    void shouldFailPaymentWithReason() {
        // Given
        Payment payment = createTestPayment();
        String failureReason = "Insufficient funds";
        String errorCode = "INSUFFICIENT_FUNDS";
        String gatewayResponse = "DECLINED";

        // When
        payment.failPayment(failureReason, errorCode, gatewayResponse);

        // Then
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(failureReason, payment.getFailureReason());
        assertEquals(errorCode, payment.getErrorCode());

        List<DomainEvent> events = payment.getUncommittedEvents();
        assertEquals(2, events.size());
        assertTrue(events.get(1) instanceof PaymentFailedEvent);
    }

    @Test
    void shouldThrowExceptionWhenCompletingNonPendingPayment() {
        // Given
        Payment payment = createTestPayment();
        payment.completePayment("txn-123", "SUCCESS");

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            payment.completePayment("txn-456", "SUCCESS");
        });
    }

    @Test
    void shouldThrowExceptionWhenFailingNonPendingPayment() {
        // Given
        Payment payment = createTestPayment();
        payment.failPayment("Test failure", "TEST_ERROR", "FAILED");

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            payment.failPayment("Another failure", "ANOTHER_ERROR", "FAILED");
        });
    }

    @Test
    void shouldRebuildFromEvents() {
        // Given
        Payment originalPayment = createTestPayment();
        originalPayment.completePayment("txn-123", "SUCCESS");
        List<DomainEvent> events = originalPayment.getUncommittedEvents();

        // When
        Payment rebuiltPayment = Payment.fromEvents(events);

        // Then
        assertEquals(originalPayment.getPaymentId(), rebuiltPayment.getPaymentId());
        assertEquals(originalPayment.getOrderId(), rebuiltPayment.getOrderId());
        assertEquals(originalPayment.getCustomerId(), rebuiltPayment.getCustomerId());
        assertEquals(originalPayment.getAmount(), rebuiltPayment.getAmount());
        assertEquals(originalPayment.getStatus(), rebuiltPayment.getStatus());
        assertEquals(originalPayment.getTransactionId(), rebuiltPayment.getTransactionId());
        assertTrue(rebuiltPayment.getUncommittedEvents().isEmpty());
    }

    @Test
    void shouldStartProcessing() {
        // Given
        Payment payment = createTestPayment();

        // When
        payment.startProcessing();

        // Then
        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenStartingProcessingFromNonPendingStatus() {
        // Given
        Payment payment = createTestPayment();
        payment.completePayment("txn-123", "SUCCESS");

        // When & Then
        assertThrows(IllegalStateException.class, payment::startProcessing);
    }

    private Payment createTestPayment() {
        return new Payment(
            "order-123",
            "customer-456", 
            new BigDecimal("25.99"),
            PaymentMethod.CREDIT_CARD,
            "card-ending-1234"
        );
    }
}