package com.restaurant.order.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom health indicator for saga processing performance and status.
 */
@Component
public class SagaProcessingHealthIndicator implements HealthIndicator {

    private final AtomicInteger activeSagas = new AtomicInteger(0);
    private final AtomicInteger completedSagas = new AtomicInteger(0);
    private final AtomicInteger failedSagas = new AtomicInteger(0);
    private final AtomicLong totalProcessingTime = new AtomicLong(0);
    private volatile LocalDateTime lastSagaCompletion = LocalDateTime.now();

    @Override
    public Health health() {
        try {
            int active = activeSagas.get();
            int completed = completedSagas.get();
            int failed = failedSagas.get();
            int total = completed + failed;
            
            double successRate = total > 0 ? (completed * 100.0) / total : 100.0;
            double averageProcessingTime = completed > 0 ? 
                totalProcessingTime.get() / (double) completed : 0.0;
            
            long minutesSinceLastCompletion = ChronoUnit.MINUTES.between(lastSagaCompletion, LocalDateTime.now());
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("activeSagas", active)
                .withDetail("completedSagas", completed)
                .withDetail("failedSagas", failed)
                .withDetail("successRate", String.format("%.2f%%", successRate))
                .withDetail("averageProcessingTime", String.format("%.2f ms", averageProcessingTime))
                .withDetail("minutesSinceLastCompletion", minutesSinceLastCompletion);
            
            // Health warnings
            if (successRate < 95.0) {
                healthBuilder.withDetail("warning", "Low saga success rate");
            }
            
            if (active > 100) {
                healthBuilder.withDetail("warning", "High number of active sagas");
            }
            
            if (averageProcessingTime > 30000) { // 30 seconds
                healthBuilder.withDetail("warning", "High average processing time");
            }
            
            if (minutesSinceLastCompletion > 10 && total > 0) {
                healthBuilder.withDetail("warning", "No recent saga completions");
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("component", "SagaProcessor")
                .build();
        }
    }

    // Methods to be called by saga orchestrator
    public void sagaStarted() {
        activeSagas.incrementAndGet();
    }

    public void sagaCompleted(long processingTimeMs) {
        activeSagas.decrementAndGet();
        completedSagas.incrementAndGet();
        totalProcessingTime.addAndGet(processingTimeMs);
        lastSagaCompletion = LocalDateTime.now();
    }

    public void sagaFailed() {
        activeSagas.decrementAndGet();
        failedSagas.incrementAndGet();
    }

    public void reset() {
        activeSagas.set(0);
        completedSagas.set(0);
        failedSagas.set(0);
        totalProcessingTime.set(0);
        lastSagaCompletion = LocalDateTime.now();
    }
}