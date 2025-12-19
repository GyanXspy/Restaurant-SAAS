package com.restaurant.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration class for Order Service.
 * Provides beans and configuration for the order service components.
 */
@Configuration
public class OrderServiceConfiguration {
    
    /**
     * Configures ObjectMapper for JSON serialization/deserialization.
     * Includes support for Java 8 time types.
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}