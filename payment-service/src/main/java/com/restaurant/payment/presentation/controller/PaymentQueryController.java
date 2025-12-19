package com.restaurant.payment.presentation.controller;

import com.restaurant.payment.domain.Payment;
import com.restaurant.payment.domain.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentQueryController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentQueryController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentDto> getPayment(@PathVariable String paymentId) {
        Optional<Payment> payment = paymentService.findPaymentById(paymentId);
        
        if (payment.isPresent()) {
            PaymentDto dto = PaymentDto.fromPayment(payment.get());
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{paymentId}/status")
    public ResponseEntity<PaymentStatusDto> getPaymentStatus(@PathVariable String paymentId) {
        Optional<Payment> payment = paymentService.findPaymentById(paymentId);
        
        if (payment.isPresent()) {
            PaymentStatusDto dto = new PaymentStatusDto(
                payment.get().getPaymentId(),
                payment.get().getStatus().name(),
                payment.get().getTransactionId(),
                payment.get().getFailureReason()
            );
            return ResponseEntity.ok(dto);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    // DTOs
    public static class PaymentDto {
        private String paymentId;
        private String orderId;
        private String customerId;
        private String amount;
        private String paymentMethod;
        private String status;
        private String transactionId;
        private String failureReason;
        private String createdAt;
        private String updatedAt;

        public static PaymentDto fromPayment(Payment payment) {
            PaymentDto dto = new PaymentDto();
            dto.paymentId = payment.getPaymentId();
            dto.orderId = payment.getOrderId();
            dto.customerId = payment.getCustomerId();
            dto.amount = payment.getAmount().toString();
            dto.paymentMethod = payment.getPaymentMethod().name();
            dto.status = payment.getStatus().name();
            dto.transactionId = payment.getTransactionId();
            dto.failureReason = payment.getFailureReason();
            dto.createdAt = payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : null;
            dto.updatedAt = payment.getUpdatedAt() != null ? payment.getUpdatedAt().toString() : null;
            return dto;
        }

        // Getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }
        public String getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class PaymentStatusDto {
        private String paymentId;
        private String status;
        private String transactionId;
        private String failureReason;

        public PaymentStatusDto(String paymentId, String status, String transactionId, String failureReason) {
            this.paymentId = paymentId;
            this.status = status;
            this.transactionId = transactionId;
            this.failureReason = failureReason;
        }

        // Getters and setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        public String getFailureReason() { return failureReason; }
        public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    }
}