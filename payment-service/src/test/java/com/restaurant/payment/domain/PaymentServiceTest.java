package com.restaurant.payment.domain;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(paymentRepository);
    }

    @Test
    void shouldInitiatePaymentSuccessfully() {
        // Given
        String orderId = "order-123";
        String customerId = "customer-456";
        BigDecimal amount = new BigDecimal("25.99");
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
        String paymentDetails = "card-ending-1234";

        // When
        Payment payment = paymentService.initiatePayment(orderId, customerId, amount, paymentMethod, paymentDetails);

        // Then
        assertNotNull(payment);
        assertEquals(orderId, payment.getOrderId());
        assertEquals(customerId, payment.getCustomerId());
        assertEquals(amount, payment.getAmount());
        assertEquals(paymentMethod, payment.getPaymentMethod());
        assertEquals(PaymentStatus.PENDING, payment.getStatus());

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void shouldThrowExceptionForInvalidOrderId() {
        // Given
        String orderId = null;
        String customerId = "customer-456";
        BigDecimal amount = new BigDecimal("25.99");
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
        String paymentDetails = "card-ending-1234";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.initiatePayment(orderId, customerId, amount, paymentMethod, paymentDetails);
        });

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldThrowExceptionForInvalidAmount() {
        // Given
        String orderId = "order-123";
        String customerId = "customer-456";
        BigDecimal amount = BigDecimal.ZERO;
        PaymentMethod paymentMethod = PaymentMethod.CREDIT_CARD;
        String paymentDetails = "card-ending-1234";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            paymentService.initiatePayment(orderId, customerId, amount, paymentMethod, paymentDetails);
        });

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldCompletePaymentSuccessfully() {
        // Given
        String paymentId = "payment-123";
        String transactionId = "txn-789";
        String gatewayResponse = "SUCCESS";
        
        Payment payment = createTestPayment();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When
        paymentService.completePayment(paymentId, transactionId, gatewayResponse);

        // Then
        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
        assertEquals(transactionId, payment.getTransactionId());
        verify(paymentRepository).save(payment);
    }

    @Test
    void shouldFailPaymentSuccessfully() {
        // Given
        String paymentId = "payment-123";
        String failureReason = "Insufficient funds";
        String errorCode = "INSUFFICIENT_FUNDS";
        String gatewayResponse = "DECLINED";
        
        Payment payment = createTestPayment();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When
        paymentService.failPayment(paymentId, failureReason, errorCode, gatewayResponse);

        // Then
        assertEquals(PaymentStatus.FAILED, payment.getStatus());
        assertEquals(failureReason, payment.getFailureReason());
        assertEquals(errorCode, payment.getErrorCode());
        verify(paymentRepository).save(payment);
    }

    @Test
    void shouldThrowExceptionWhenPaymentNotFound() {
        // Given
        String paymentId = "non-existent-payment";
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(PaymentService.PaymentNotFoundException.class, () -> {
            paymentService.completePayment(paymentId, "txn-123", "SUCCESS");
        });

        verify(paymentRepository, never()).save(any(Payment.class));
    }

    @Test
    void shouldStartPaymentProcessing() {
        // Given
        String paymentId = "payment-123";
        Payment payment = createTestPayment();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When
        paymentService.startPaymentProcessing(paymentId);

        // Then
        assertEquals(PaymentStatus.PROCESSING, payment.getStatus());
        verify(paymentRepository).save(payment);
    }

    @Test
    void shouldFindPaymentById() {
        // Given
        String paymentId = "payment-123";
        Payment payment = createTestPayment();
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        // When
        Optional<Payment> result = paymentService.findPaymentById(paymentId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(payment, result.get());
        verify(paymentRepository).findById(paymentId);
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