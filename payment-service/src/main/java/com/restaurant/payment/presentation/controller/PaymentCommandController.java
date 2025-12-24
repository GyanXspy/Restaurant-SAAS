package com.restaurant.payment.presentation.controller;

import com.restaurant.payment.domain.Payment;
import com.restaurant.payment.domain.PaymentMethod;
import com.restaurant.payment.domain.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/payments")
public class PaymentCommandController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentCommandController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(@RequestBody InitiatePaymentRequest request) {
        try {
            Payment payment = paymentService.initiatePayment(
                request.getOrderId(),
                request.getCustomerId(),
                request.getAmount(),
                request.getPaymentMethod(),
                request.getPaymentDetails()
            );
            
            PaymentResponse response = new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getStatus().name(),
                "Payment initiated successfully"
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{paymentId}/complete")
    public ResponseEntity<String> completePayment(
            @PathVariable String paymentId,
            @RequestBody CompletePaymentRequest request) {
        try {
            paymentService.completePayment(paymentId, request.getTransactionId(), request.getGatewayResponse());
            return ResponseEntity.ok("Payment completed successfully");
        } catch (PaymentService.PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{paymentId}/fail")
    public ResponseEntity<String> failPayment(
            @PathVariable String paymentId,
            @RequestBody FailPaymentRequest request) {
        try {
            paymentService.failPayment(
                paymentId,
                request.getFailureReason(),
                request.getErrorCode(),
                request.getGatewayResponse()
            );
            return ResponseEntity.ok("Payment marked as failed");
        } catch (PaymentService.PaymentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Request DTOs
    public static class InitiatePaymentRequest {
        private String orderId;
        private String customerId;
        private BigDecimal amount;
        private PaymentMethod paymentMethod;
        private String paymentDetails;

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getPaymentDetails() { return paymentDetails; }
        public void setPaymentDetails(String paymentDetails) { this.paymentDetails = paymentDetails; }
    }

    public static class CompletePaymentRequest {
        private String transactionId;
        private String gatewayResponse;

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getGatewayResponse() { return gatewayResponse; }
        public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    }

    public static class FailPaymentRequest {
        private String failureReason;
        private String errorCode;
        private String gatewayResponse;

        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        public String getErrorCode() { return errorCode; }
        public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
        public String getGatewayResponse() { return gatewayResponse; }
        public void setGatewayResponse(String gatewayResponse) { this.gatewayResponse = gatewayResponse; }
    }

    public static class PaymentResponse {
        private String paymentId;
        private String orderId;
        private String status;
        private String message;

        public PaymentResponse(String paymentId, String orderId, String status, String message) {
            this.paymentId = paymentId;
            this.orderId = orderId;
            this.status = status;
            this.message = message;
        }

        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
