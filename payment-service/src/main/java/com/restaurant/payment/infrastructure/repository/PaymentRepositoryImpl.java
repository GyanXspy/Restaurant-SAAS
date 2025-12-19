package com.restaurant.payment.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.restaurant.events.DomainEvent;
import com.restaurant.payment.domain.Payment;
import com.restaurant.payment.domain.PaymentRepository;
import com.restaurant.payment.infrastructure.eventstore.PaymentEventStore;

@Repository
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentEventStore eventStore;

    @Autowired
    public PaymentRepositoryImpl(PaymentEventStore eventStore) {
        this.eventStore = eventStore;
    }

    @Override
    @Transactional
    public void save(Payment payment) {
        List<DomainEvent> uncommittedEvents = payment.getUncommittedEvents();
        if (!uncommittedEvents.isEmpty()) {
            eventStore.saveEvents(payment.getPaymentId(), uncommittedEvents, payment.getVersion());
            payment.setVersion(payment.getVersion() + uncommittedEvents.size());
            payment.markEventsAsCommitted();
        }
    }

    @Override
    public Optional<Payment> findById(String paymentId) {
        List<DomainEvent> events = eventStore.getEvents(paymentId);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        
        Payment payment = Payment.fromEvents(events);
        return Optional.of(payment);
    }

    @Override
    public Optional<Payment> findByOrderId(String orderId) {
        // Note: This is a simplified implementation. In a real system, you might need
        // an index table or search through events to find payments by order ID
        // For now, this method is not fully implemented as it requires additional infrastructure
        throw new UnsupportedOperationException("Finding payment by order ID requires additional indexing implementation");
    }
}