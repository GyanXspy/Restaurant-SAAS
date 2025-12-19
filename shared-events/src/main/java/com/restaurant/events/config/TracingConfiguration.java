package com.restaurant.events.config;

import brave.sampler.Sampler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for distributed tracing across microservices.
 */
@Configuration
@ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfiguration {

    @Bean
    public Sampler alwaysSampler() {
        // In production, you might want to use a rate-limited sampler
        // For development and testing, sample all traces
        return Sampler.create(1.0f);
    }
}