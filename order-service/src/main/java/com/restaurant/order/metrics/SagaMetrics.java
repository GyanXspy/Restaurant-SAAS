package com.restaurant.order.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom metrics for saga orchestration performance and reliability.
 */
@Component
public class SagaMetrics {

    private final Counter sagasStarted;
    private final Counter sagasCompleted;
    private final Counter sagasFailed;
    private final Counter sagaCompensations;
    private final Counter sagaTimeouts;
    private final Timer sagaExecutionTime;
    private final Timer sagaStepExecutionTime;
    private final AtomicInteger activeSagas = new AtomicInteger(0);

    public SagaMetrics(MeterRegistry meterRegistry) {
        this.sagasStarted = Counter.builder("sagas.started.total")
            .description("Total number of sagas started")
            .tag("component", "saga-orchestrator")
            .register(meterRegistry);

        this.sagasCompleted = Counter.builder("sagas.completed.total")
            .description("Total number of sagas completed successfully")
            .tag("component", "saga-orchestrator")
            .register(meterRegistry);

        this.sagasFailed = Counter.builder("sagas.failed.total")
            .description("Total number of sagas that failed")
            .tag("component", "saga-orchestrator")
            .register(meterRegistry);

        this.sagaCompensations = Counter.builder("sagas.compensations.total")
            .description("Total number of saga compensations executed")
            .tag("component", "saga-orchestrator")
            .register(meterRegistry);

        this.sagaTimeouts = Counter.builder("sagas.timeouts.total")
            .description("Total number of saga timeouts")
            .tag("component", "saga-orchestrator")
            .register(meterRegistry);

        this.sagaExecutionTime = Timer.builder("sagas.execution.duration")
            .description("Time taken to execute complete sagas")
            .tag("component", "saga-orchestrator")
            .register(meterRegistry);

        this.sagaStepExecutionTime = Timer.builder("sagas.step.execution.duration")
            .description("Time taken to execute individual saga steps")
            .tag("component", "saga-orchestrator")
            .register(meterRegistry);

        // Gauge for active sagas
        Gauge.builder("sagas.active.current")
            .description("Current number of active sagas")
            .tag("component", "saga-orchestrator")
            .register(meterRegistry, activeSagas, AtomicInteger::get);
    }

    public void recordSagaStarted(String sagaType) {
        sagasStarted.increment("saga.type", sagaType);
        activeSagas.incrementAndGet();
    }

    public void recordSagaCompleted(String sagaType, Duration executionTime) {
        sagasCompleted.increment("saga.type", sagaType);
        activeSagas.decrementAndGet();
        sagaExecutionTime
            .tag("saga.type", sagaType)
            .record(executionTime.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void recordSagaFailed(String sagaType, String failureReason) {
        sagasFailed.increment(
            "saga.type", sagaType,
            "failure.reason", failureReason
        );
        activeSagas.decrementAndGet();
    }

    public void recordSagaCompensation(String sagaType, String compensationStep) {
        sagaCompensations.increment(
            "saga.type", sagaType,
            "compensation.step", compensationStep
        );
    }

    public void recordSagaTimeout(String sagaType, String timeoutStep) {
        sagaTimeouts.increment(
            "saga.type", sagaType,
            "timeout.step", timeoutStep
        );
        activeSagas.decrementAndGet();
    }

    public Timer.Sample startSagaStepTimer() {
        return Timer.start();
    }

    public void recordSagaStepExecutionTime(Timer.Sample sample, String sagaType, String stepName) {
        sample.stop(sagaStepExecutionTime
            .tag("saga.type", sagaType)
            .tag("step.name", stepName));
    }

    public void recordSagaStepDuration(Duration duration, String sagaType, String stepName) {
        sagaStepExecutionTime
            .tag("saga.type", sagaType)
            .tag("step.name", stepName)
            .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public int getActiveSagaCount() {
        return activeSagas.get();
    }
}