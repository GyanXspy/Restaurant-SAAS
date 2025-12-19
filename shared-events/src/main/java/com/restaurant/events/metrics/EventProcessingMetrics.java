package com.restaurant.events.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Custom metrics for event processing performance and reliability.
 */
@Component
public class EventProcessingMetrics {

    private final Counter eventsPublished;
    private final Counter eventsConsumed;
    private final Counter eventProcessingErrors;
    private final Counter eventRetries;
    private final Timer eventProcessingTime;
    private final Timer eventPublishingTime;

    public EventProcessingMetrics(MeterRegistry meterRegistry) {
        this.eventsPublished = Counter.builder("events.published.total")
            .description("Total number of events published")
            .tag("component", "event-publisher")
            .register(meterRegistry);

        this.eventsConsumed = Counter.builder("events.consumed.total")
            .description("Total number of events consumed")
            .tag("component", "event-consumer")
            .register(meterRegistry);

        this.eventProcessingErrors = Counter.builder("events.processing.errors.total")
            .description("Total number of event processing errors")
            .tag("component", "event-processor")
            .register(meterRegistry);

        this.eventRetries = Counter.builder("events.retries.total")
            .description("Total number of event processing retries")
            .tag("component", "event-processor")
            .register(meterRegistry);

        this.eventProcessingTime = Timer.builder("events.processing.duration")
            .description("Time taken to process events")
            .tag("component", "event-processor")
            .register(meterRegistry);

        this.eventPublishingTime = Timer.builder("events.publishing.duration")
            .description("Time taken to publish events")
            .tag("component", "event-publisher")
            .register(meterRegistry);
    }

    public void recordEventPublished(String eventType) {
        eventsPublished.increment(
            "event.type", eventType
        );
    }

    public void recordEventConsumed(String eventType) {
        eventsConsumed.increment(
            "event.type", eventType
        );
    }

    public void recordEventProcessingError(String eventType, String errorType) {
        eventProcessingErrors.increment(
            "event.type", eventType,
            "error.type", errorType
        );
    }

    public void recordEventRetry(String eventType, int attemptNumber) {
        eventRetries.increment(
            "event.type", eventType,
            "attempt", String.valueOf(attemptNumber)
        );
    }

    public Timer.Sample startEventProcessingTimer() {
        return Timer.start();
    }

    public void recordEventProcessingTime(Timer.Sample sample, String eventType) {
        sample.stop(eventProcessingTime.tag("event.type", eventType));
    }

    public Timer.Sample startEventPublishingTimer() {
        return Timer.start();
    }

    public void recordEventPublishingTime(Timer.Sample sample, String eventType) {
        sample.stop(eventPublishingTime.tag("event.type", eventType));
    }

    public void recordEventProcessingDuration(Duration duration, String eventType) {
        eventProcessingTime
            .tag("event.type", eventType)
            .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }

    public void recordEventPublishingDuration(Duration duration, String eventType) {
        eventPublishingTime
            .tag("event.type", eventType)
            .record(duration.toMillis(), TimeUnit.MILLISECONDS);
    }
}