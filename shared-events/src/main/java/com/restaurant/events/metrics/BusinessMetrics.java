package com.restaurant.events.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Business metrics collection for monitoring key business events
 */
@Component
public class BusinessMetrics {

    private final Counter ordersCreated;
    private final Counter ordersConfirmed;
    private final Counter ordersCancelled;
    private final Counter paymentsInitiated;
    private final Counter paymentsCompleted;
    private final Counter paymentsFailed;
    private final Counter cartValidationsRequested;
    private final Counter cartValidationsCompleted;
    private final Counter cartValidationsFailed;
    private final Timer sagaProcessingDuration;
    private final Timer orderProcessingDuration;
    private final Timer paymentProcessingDuration;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        // Order metrics
        this.ordersCreated = Counter.builder("orders.created.total")
                .description("Total number of orders created")
                .register(meterRegistry);
        
        this.ordersConfirmed = Counter.builder("orders.confirmed.total")
                .description("Total number of orders confirmed")
                .register(meterRegistry);
        
        this.ordersCancelled = Counter.builder("orders.cancelled.total")
                .description("Total number of orders cancelled")
                .register(meterRegistry);

        // Payment metrics
        this.paymentsInitiated = Counter.builder("payments.initiated.total")
                .description("Total number of payments initiated")
                .register(meterRegistry);
        
        this.paymentsCompleted = Counter.builder("payments.completed.total")
                .description("Total number of payments completed")
                .register(meterRegistry);
        
        this.paymentsFailed = Counter.builder("payments.failed.total")
                .description("Total number of payments failed")
                .register(meterRegistry);

        // Cart validation metrics
        this.cartValidationsRequested = Counter.builder("cart.validations.requested.total")
                .description("Total number of cart validations requested")
                .register(meterRegistry);
        
        this.cartValidationsCompleted = Counter.builder("cart.validations.completed.total")
                .description("Total number of cart validations completed successfully")
                .register(meterRegistry);
        
        this.cartValidationsFailed = Counter.builder("cart.validations.failed.total")
                .description("Total number of cart validations failed")
                .register(meterRegistry);

        // Duration metrics
        this.sagaProcessingDuration = Timer.builder("saga.processing.duration")
                .description("Time taken to process saga from start to completion")
                .register(meterRegistry);
        
        this.orderProcessingDuration = Timer.builder("order.processing.duration")
                .description("Time taken to process an order from creation to confirmation")
                .register(meterRegistry);
        
        this.paymentProcessingDuration = Timer.builder("payment.processing.duration")
                .description("Time taken to process a payment")
                .register(meterRegistry);
    }

    // Order metrics methods
    public void incrementOrdersCreated() {
        ordersCreated.increment();
    }

    public void incrementOrdersConfirmed() {
        ordersConfirmed.increment();
    }

    public void incrementOrdersCancelled() {
        ordersCancelled.increment();
    }

    // Payment metrics methods
    public void incrementPaymentsInitiated() {
        paymentsInitiated.increment();
    }

    public void incrementPaymentsCompleted() {
        paymentsCompleted.increment();
    }

    public void incrementPaymentsFailed() {
        paymentsFailed.increment();
    }

    // Cart validation metrics methods
    public void incrementCartValidationsRequested() {
        cartValidationsRequested.increment();
    }

    public void incrementCartValidationsCompleted() {
        cartValidationsCompleted.increment();
    }

    public void incrementCartValidationsFailed() {
        cartValidationsFailed.increment();
    }

    // Timer methods
    public Timer.Sample startSagaProcessingTimer() {
        return Timer.start(sagaProcessingDuration);
    }

    public Timer.Sample startOrderProcessingTimer() {
        return Timer.start(orderProcessingDuration);
    }

    public Timer.Sample startPaymentProcessingTimer() {
        return Timer.start(paymentProcessingDuration);
    }
}