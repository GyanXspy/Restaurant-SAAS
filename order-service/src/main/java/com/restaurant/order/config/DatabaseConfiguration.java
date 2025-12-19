package com.restaurant.order.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.restaurant.order.infrastructure.repository")
@EntityScan(basePackages = "com.restaurant.order.infrastructure.entity")
@EnableTransactionManagement
public class DatabaseConfiguration {
}