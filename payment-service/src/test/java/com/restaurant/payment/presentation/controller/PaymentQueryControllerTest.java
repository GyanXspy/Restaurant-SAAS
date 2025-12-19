package com.restaurant.payment.presentation.controller;

import com.restaurant.payment.domain.Payment;
import com.restaurant.payment.domain.PaymentMethod;
import com.restaurant.payment.domain.PaymentService;
import com.restaurant.payment.domain.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentQueryController.class)
class PaymentQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentService paymentService;

    @Test
    void shouldReturnPaymentWhenFound() throws Exception {
        // Given
        String paymentId = "payment-123";
        Payment payment = new Payment(
            "order-456",
            "customer-789",
            new BigDecimal("25.99"),
            PaymentMethod.CREDIT_CARD,
            "card-ending-1234"
        );

        when(paymentService.findPaymentById(paymentId)).thenReturn(Optional.of(payment));

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getPaymentId()))
                .andExpect(jsonPath("$.orderId").value("order-456"))
                .andExpect(jsonPath("$.customerId").value("customer-789"))
                .andExpect(jsonPath("$.amount").value("25.99"))
                .andExpect(jsonPath("$.paymentMethod").value("CREDIT_CARD"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void shouldReturnNotFoundWhenPaymentDoesNotExist() throws Exception {
        // Given
        String paymentId = "non-existent-payment";
        when(paymentService.findPaymentById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}", paymentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnPaymentStatusWhenFound() throws Exception {
        // Given
        String paymentId = "payment-123";
        Payment payment = new Payment(
            "order-456",
            "customer-789",
            new BigDecimal("25.99"),
            PaymentMethod.CREDIT_CARD,
            "card-ending-1234"
        );
        payment.completePayment("TXN-12345", "SUCCESS");

        when(paymentService.findPaymentById(paymentId)).thenReturn(Optional.of(payment));

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}/status", paymentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(payment.getPaymentId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transactionId").value("TXN-12345"));
    }

    @Test
    void shouldReturnNotFoundForStatusWhenPaymentDoesNotExist() throws Exception {
        // Given
        String paymentId = "non-existent-payment";
        when(paymentService.findPaymentById(paymentId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/payments/{paymentId}/status", paymentId))
                .andExpect(status().isNotFound());
    }
}