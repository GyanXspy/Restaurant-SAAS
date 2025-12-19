package com.restaurant.payment.domain;

import java.util.Optional;

public interface PaymentRepository {
    
    void save(Payment payment);
    
    Optional<Payment> findById(String paymentId);
    
    Optional<Payment> findByOrderId(String orderId);
}