package com.restaurant.events.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.boot.actuator.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for monitoring, metrics, and observability.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
public class MonitoringConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config()
                .commonTags("application", "restaurant-ordering-system")
                .commonTags("version", "1.0.0")
                .meterFilter(MeterFilter.deny(id -> {
                    String name = id.getName();
                    // Filter out noisy metrics
                    return name.startsWith("jvm.gc.pause") ||
                           name.startsWith("jvm.memory.committed") ||
                           name.startsWith("system.cpu.count");
                }));
        };
    }

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> configureMetrics() {
        return registry -> {
            // Configure histogram buckets for timing metrics
            registry.config()
                .meterFilter(MeterFilter.maximumExpectedValue("http.server.requests", 
                    java.time.Duration.ofSeconds(10)))
                .meterFilter(MeterFilter.maximumExpectedValue("events.processing.duration", 
                    java.time.Duration.ofSeconds(30)))
                .meterFilter(MeterFilter.maximumExpectedValue("sagas.execution.duration", 
                    java.time.Duration.ofMinutes(5)));
        };
    }
}