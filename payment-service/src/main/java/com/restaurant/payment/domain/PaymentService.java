package com.restaurant.payment.domain;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public Payment initiatePayment(String orderId, String customerId, BigDecimal amount, 
                                 PaymentMethod paymentMethod, String paymentDetails) {
        
        // Validate input
        validatePaymentRequest(orderId, customerId, amount, paymentMethod);
        
        // Create new payment aggregate
        Payment payment = new Payment(orderId, customerId, amount, paymentMethod, paymentDetails);
        
        // Save payment (this will persist the PaymentInitiatedEvent)
        paymentRepository.save(payment);
        
        return payment;
    }

    @Transactional
    public void completePayment(String paymentId, String transactionId, String gatewayResponse) {
        Payment payment = getPaymentById(paymentId);
        
        payment.completePayment(transactionId, gatewayResponse);
        paymentRepository.save(payment);
    }

    @Transactional
    public void failPayment(String paymentId, String failureReason, String errorCode, String gatewayResponse) {
        Payment payment = getPaymentById(paymentId);
        
        payment.failPayment(failureReason, errorCode, gatewayResponse);
        paymentRepository.save(payment);
    }

    @Transactional
    public void startPaymentProcessing(String paymentId) {
        Payment payment = getPaymentById(paymentId);
        
        payment.startProcessing();
        paymentRepository.save(payment);
    }

    public Optional<Payment> findPaymentById(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId);
    }

    private Payment getPaymentById(String paymentId) {
        return paymentRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));
    }

    private void validatePaymentRequest(String orderId, String customerId, BigDecimal amount, PaymentMethod paymentMethod) {
        if (orderId == null || orderId.trim().isEmpty()) {
            throw new IllegalArgumentException("Order ID cannot be null or empty");
        }
        
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID cannot be null or empty");
        }
        
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        if (paymentMethod == null) {
            throw new IllegalArgumentException("Payment method cannot be null");
        }
    }

    public static class PaymentNotFoundException extends RuntimeException {
        public PaymentNotFoundException(String message) {
            super(message);
        }
    }
}