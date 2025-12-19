package com.restaurant.payment.application.eventhandler;

import com.restaurant.events.PaymentInitiationRequestedEvent;
import com.restaurant.events.PaymentProcessingCompletedEvent;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.payment.domain.Payment;
import com.restaurant.payment.domain.PaymentMethod;
import com.restaurant.payment.domain.PaymentService;
import com.restaurant.payment.infrastructure.gateway.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentInitiationEventHandlerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Acknowledgment acknowledgment;

    private PaymentInitiationEventHandler eventHandler;

    @BeforeEach
    void setUp() {
        eventHandler = new PaymentInitiationEventHandler(paymentService, paymentGateway, eventPublisher);
    }

    @Test
    void shouldHandleSuccessfulPaymentInitiation() {
        // Given
        PaymentInitiationRequestedEvent event = createTestEvent();
        Payment mockPayment = createMockPayment();
        PaymentGateway.PaymentResult successResult = PaymentGateway.PaymentResult.success(
            "TXN-12345", "{\"status\":\"SUCCESS\"}"
        );

        when(paymentService.initiatePayment(anyString(), anyString(), any(BigDecimal.class), 
                                          any(PaymentMethod.class), anyString()))
            .thenReturn(mockPayment);
        when(paymentGateway.processPayment(any(PaymentGateway.PaymentRequest.class)))
            .thenReturn(successResult);

        // When
        eventHandler.handlePaymentInitiationRequested(event, "payment-initiation-requested", 0, 0L, acknowledgment);

        // Then
        verify(paymentService).initiatePayment(
            event.getOrderId(),
            event.getCustomerId(),
            event.getAmount(),
            PaymentMethod.CREDIT_CARD,
            "Payment for order " + event.getOrderId()
        );
        verify(paymentService).startPaymentProcessing(mockPayment.getPaymentId());
        verify(paymentService).completePayment(
            mockPayment.getPaymentId(),
            "TXN-12345",
            "{\"status\":\"SUCCESS\"}"
        );

        // Verify success event is published
        ArgumentCaptor<PaymentProcessingCompletedEvent> eventCaptor = 
            ArgumentCaptor.forClass(PaymentProcessingCompletedEvent.class);
        verify(eventPublisher).publish(eq("payment-processing-completed"), eventCaptor.capture());

        PaymentProcessingCompletedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(mockPayment.getPaymentId(), publishedEvent.getPaymentId());
        assertEquals(event.getOrderId(), publishedEvent.getOrderId());
        assertEquals(PaymentProcessingCompletedEvent.PaymentStatus.COMPLETED, publishedEvent.getStatus());
        assertNull(publishedEvent.getFailureReason());

        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleFailedPaymentProcessing() {
        // Given
        PaymentInitiationRequestedEvent event = createTestEvent();
        Payment mockPayment = createMockPayment();
        PaymentGateway.PaymentResult failureResult = PaymentGateway.PaymentResult.failure(
            "INSUFFICIENT_FUNDS", "Insufficient funds", "{\"status\":\"DECLINED\"}"
        );

        when(paymentService.initiatePayment(anyString(), anyString(), any(BigDecimal.class), 
                                          any(PaymentMethod.class), anyString()))
            .thenReturn(mockPayment);
        when(paymentGateway.processPayment(any(PaymentGateway.PaymentRequest.class)))
            .thenReturn(failureResult);

        // When
        eventHandler.handlePaymentInitiationRequested(event, "payment-initiation-requested", 0, 0L, acknowledgment);

        // Then
        verify(paymentService).initiatePayment(
            event.getOrderId(),
            event.getCustomerId(),
            event.getAmount(),
            PaymentMethod.CREDIT_CARD,
            "Payment for order " + event.getOrderId()
        );
        verify(paymentService).startPaymentProcessing(mockPayment.getPaymentId());
        verify(paymentService).failPayment(
            mockPayment.getPaymentId(),
            "Insufficient funds",
            "INSUFFICIENT_FUNDS",
            "{\"status\":\"DECLINED\"}"
        );

        // Verify failure event is published
        ArgumentCaptor<PaymentProcessingCompletedEvent> eventCaptor = 
            ArgumentCaptor.forClass(PaymentProcessingCompletedEvent.class);
        verify(eventPublisher).publish(eq("payment-processing-completed"), eventCaptor.capture());

        PaymentProcessingCompletedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(mockPayment.getPaymentId(), publishedEvent.getPaymentId());
        assertEquals(event.getOrderId(), publishedEvent.getOrderId());
        assertEquals(PaymentProcessingCompletedEvent.PaymentStatus.FAILED, publishedEvent.getStatus());
        assertEquals("Insufficient funds", publishedEvent.getFailureReason());

        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleTimeoutFailure() {
        // Given
        PaymentInitiationRequestedEvent event = createTestEvent();
        Payment mockPayment = createMockPayment();
        PaymentGateway.PaymentResult timeoutResult = PaymentGateway.PaymentResult.failure(
            "GATEWAY_TIMEOUT", "Gateway timeout", "{\"status\":\"TIMEOUT\"}"
        );

        when(paymentService.initiatePayment(anyString(), anyString(), any(BigDecimal.class), 
                                          any(PaymentMethod.class), anyString()))
            .thenReturn(mockPayment);
        when(paymentGateway.processPayment(any(PaymentGateway.PaymentRequest.class)))
            .thenReturn(timeoutResult);

        // When
        eventHandler.handlePaymentInitiationRequested(event, "payment-initiation-requested", 0, 0L, acknowledgment);

        // Then
        verify(paymentService).failPayment(
            mockPayment.getPaymentId(),
            "Gateway timeout",
            "GATEWAY_TIMEOUT",
            "{\"status\":\"TIMEOUT\"}"
        );

        // Verify timeout event is published
        ArgumentCaptor<PaymentProcessingCompletedEvent> eventCaptor = 
            ArgumentCaptor.forClass(PaymentProcessingCompletedEvent.class);
        verify(eventPublisher).publish(eq("payment-processing-completed"), eventCaptor.capture());

        PaymentProcessingCompletedEvent publishedEvent = eventCaptor.getValue();
        assertEquals(PaymentProcessingCompletedEvent.PaymentStatus.TIMEOUT, publishedEvent.getStatus());

        verify(acknowledgment).acknowledge();
    }

    @Test
    void shouldHandleProcessingException() {
        // Given
        PaymentInitiationRequestedEvent event = createTestEvent();
        
        when(paymentService.initiatePayment(anyString(), anyString(), any(BigDecimal.class), 
                                          any(PaymentMethod.class), anyString()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When
        eventHandler.handlePaymentInitiationRequested(event, "payment-initiation-requested", 0, 0L, acknowledgment);

        // Then
        // Verify failure event is published even when processing fails
        ArgumentCaptor<PaymentProcessingCompletedEvent> eventCaptor = 
            ArgumentCaptor.forClass(PaymentProcessingCompletedEvent.class);
        verify(eventPublisher).publish(eq("payment-processing-completed"), eventCaptor.capture());

        PaymentProcessingCompletedEvent publishedEvent = eventCaptor.getValue();
        assertNull(publishedEvent.getPaymentId()); // No payment ID since creation failed
        assertEquals(event.getOrderId(), publishedEvent.getOrderId());
        assertEquals(PaymentProcessingCompletedEvent.PaymentStatus.FAILED, publishedEvent.getStatus());
        assertNotNull(publishedEvent.getFailureReason());

        verify(acknowledgment).acknowledge();
    }

    private PaymentInitiationRequestedEvent createTestEvent() {
        return new PaymentInitiationRequestedEvent(
            "saga-123",
            "order-456",
            "customer-789",
            new BigDecimal("25.99"),
            "CREDIT_CARD",
            1
        );
    }

    private Payment createMockPayment() {
        Payment payment = new Payment(
            "order-456",
            "customer-789",
            new BigDecimal("25.99"),
            PaymentMethod.CREDIT_CARD,
            "Payment for order order-456"
        );
        return payment;
    }
}