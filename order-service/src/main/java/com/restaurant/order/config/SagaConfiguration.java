package com.restaurant.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.order.saga.OrderSagaRepository;
import com.restaurant.order.saga.OrderSagaRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Configuration class for Saga-related components.
 * Ensures proper setup of saga orchestration infrastructure.
 */
@Configuration
public class SagaConfiguration {
    
    /**
     * Creates the OrderSagaRepository bean for saga state persistence.
     */
    @Bean
    public OrderSagaRepository orderSagaRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        return new OrderSagaRepositoryImpl(jdbcTemplate, objectMapper);
    }
}