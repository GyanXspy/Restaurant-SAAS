package com.restaurant.payment.application.eventhandler;

import com.restaurant.events.PaymentInitiationRequestedEvent;
import com.restaurant.events.PaymentProcessingCompletedEvent;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.payment.domain.Payment;
import com.restaurant.payment.domain.PaymentMethod;
import com.restaurant.payment.domain.PaymentService;
import com.restaurant.payment.infrastructure.gateway.PaymentGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class PaymentInitiationEventHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentInitiationEventHandler.class);
    
    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final EventPublisher eventPublisher;

    @Autowired
    public PaymentInitiationEventHandler(PaymentService paymentService, 
                                       @Qualifier("resilientPaymentGateway") PaymentGateway paymentGateway,
                                       EventPublisher eventPublisher) {
        this.paymentService = paymentService;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(topics = "payment-initiation-requested", groupId = "payment-service")
    public void handlePaymentInitiationRequested(@Payload PaymentInitiationRequestedEvent event,
                                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                                @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                                @Header(KafkaHeaders.OFFSET) long offset,
                                                Acknowledgment acknowledgment) {
        
        logger.info("Received PaymentInitiationRequestedEvent for orderId: {}, amount: {}", 
                   event.getOrderId(), event.getAmount());
        
        try {
            // Process the payment initiation request
            processPaymentInitiation(event);
            
            // Acknowledge the message after successful processing
            acknowledgment.acknowledge();
            
            logger.info("Successfully processed PaymentInitiationRequestedEvent for orderId: {}", 
                       event.getOrderId());
            
        } catch (Exception e) {
            logger.error("Failed to process PaymentInitiationRequestedEvent for orderId: {}", 
                        event.getOrderId(), e);
            
            // Publish failure event
            publishPaymentFailure(event, "PROCESSING_ERROR", e.getMessage());
            
            // Acknowledge to prevent reprocessing (move to DLQ if configured)
            acknowledgment.acknowledge();
        }
    }
    
    private void processPaymentInitiation(PaymentInitiationRequestedEvent event) {
        // Convert string payment method to enum
        PaymentMethod paymentMethod = PaymentMethod.valueOf(event.getPaymentMethod().toUpperCase());
        
        // Create payment aggregate
        Payment payment = paymentService.initiatePayment(
            event.getOrderId(),
            event.getCustomerId(),
            event.getAmount(),
            paymentMethod,
            "Payment for order " + event.getOrderId()
        );
        
        logger.info("Created payment with ID: {} for order: {}", payment.getPaymentId(), event.getOrderId());
        
        // Start payment processing
        paymentService.startPaymentProcessing(payment.getPaymentId());
        
        // Process payment through gateway
        PaymentGateway.PaymentRequest gatewayRequest = new PaymentGateway.PaymentRequest(
            payment.getPaymentId(),
            payment.getAmount(),
            payment.getPaymentMethod(),
            payment.getPaymentDetails(),
            payment.getCustomerId()
        );
        
        PaymentGateway.PaymentResult gatewayResult = paymentGateway.processPayment(gatewayRequest);
        
        // Handle gateway result
        if (gatewayResult.isSuccess()) {
            handleSuccessfulPayment(payment, gatewayResult, event.getAggregateId());
        } else {
            handleFailedPayment(payment, gatewayResult, event.getAggregateId());
        }
    }
    
    private void handleSuccessfulPayment(Payment payment, PaymentGateway.PaymentResult result, String sagaId) {
        // Complete payment in domain
        paymentService.completePayment(
            payment.getPaymentId(),
            result.getTransactionId(),
            result.getGatewayResponse()
        );
        
        // Publish success event
        PaymentProcessingCompletedEvent completedEvent = new PaymentProcessingCompletedEvent(
            sagaId,
            payment.getPaymentId(),
            payment.getOrderId(),
            payment.getAmount(),
            PaymentProcessingCompletedEvent.PaymentStatus.COMPLETED,
            null,
            1
        );
        
        eventPublisher.publish("payment-processing-completed", completedEvent);
        
        logger.info("Payment completed successfully for paymentId: {}, transactionId: {}", 
                   payment.getPaymentId(), result.getTransactionId());
    }
    
    private void handleFailedPayment(Payment payment, PaymentGateway.PaymentResult result, String sagaId) {
        // Fail payment in domain
        paymentService.failPayment(
            payment.getPaymentId(),
            result.getErrorMessage(),
            result.getErrorCode(),
            result.getGatewayResponse()
        );
        
        // Determine failure status
        PaymentProcessingCompletedEvent.PaymentStatus status = 
            "GATEWAY_TIMEOUT".equals(result.getErrorCode()) ? 
                PaymentProcessingCompletedEvent.PaymentStatus.TIMEOUT : 
                PaymentProcessingCompletedEvent.PaymentStatus.FAILED;
        
        // Publish failure event
        PaymentProcessingCompletedEvent completedEvent = new PaymentProcessingCompletedEvent(
            sagaId,
            payment.getPaymentId(),
            payment.getOrderId(),
            payment.getAmount(),
            status,
            result.getErrorMessage(),
            1
        );
        
        eventPublisher.publish("payment-processing-completed", completedEvent);
        
        logger.warn("Payment failed for paymentId: {}, reason: {}", 
                   payment.getPaymentId(), result.getErrorMessage());
    }
    
    private void publishPaymentFailure(PaymentInitiationRequestedEvent originalEvent, String errorCode, String errorMessage) {
        try {
            PaymentProcessingCompletedEvent failureEvent = new PaymentProcessingCompletedEvent(
                originalEvent.getAggregateId(),
                null, // No payment ID since creation failed
                originalEvent.getOrderId(),
                originalEvent.getAmount(),
                PaymentProcessingCompletedEvent.PaymentStatus.FAILED,
                errorMessage,
                1
            );
            
            eventPublisher.publish("payment-processing-completed", failureEvent);
            
        } catch (Exception e) {
            logger.error("Failed to publish payment failure event for orderId: {}", 
                        originalEvent.getOrderId(), e);
        }
    }
}