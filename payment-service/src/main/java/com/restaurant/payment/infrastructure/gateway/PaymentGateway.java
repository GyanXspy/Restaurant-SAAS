package com.restaurant.payment.infrastructure.gateway;

import java.math.BigDecimal;

import com.restaurant.payment.domain.PaymentMethod;

public interface PaymentGateway {
    
    PaymentResult processPayment(PaymentRequest request);
    
    class PaymentRequest {
        private final String paymentId;
        private final BigDecimal amount;
        private final PaymentMethod paymentMethod;
        private final String paymentDetails;
        private final String customerId;

        public PaymentRequest(String paymentId, BigDecimal amount, PaymentMethod paymentMethod, 
                            String paymentDetails, String customerId) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.paymentMethod = paymentMethod;
            this.paymentDetails = paymentDetails;
            this.customerId = customerId;
        }

        public String getPaymentId() { return paymentId; }
        public BigDecimal getAmount() { return amount; }
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public String getPaymentDetails() { return paymentDetails; }
        public String getCustomerId() { return customerId; }
    }
    
    class PaymentResult {
        private final boolean success;
        private final String transactionId;
        private final String errorCode;
        private final String errorMessage;
        private final String gatewayResponse;

        private PaymentResult(boolean success, String transactionId, String errorCode, 
                            String errorMessage, String gatewayResponse) {
            this.success = success;
            this.transactionId = transactionId;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.gatewayResponse = gatewayResponse;
        }

        public static PaymentResult success(String transactionId, String gatewayResponse) {
            return new PaymentResult(true, transactionId, null, null, gatewayResponse);
        }

        public static PaymentResult failure(String errorCode, String errorMessage, String gatewayResponse) {
            return new PaymentResult(false, null, errorCode, errorMessage, gatewayResponse);
        }

        public boolean isSuccess() { return success; }
        public String getTransactionId() { return transactionId; }
        public String getErrorCode() { return errorCode; }
        public String getErrorMessage() { return errorMessage; }
        public String getGatewayResponse() { return gatewayResponse; }
    }
}