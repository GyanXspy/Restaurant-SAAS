package com.restaurant.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Integration test application that starts all microservices in a single context
 * for end-to-end testing.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.restaurant.user",
    "com.restaurant.restaurant", 
    "com.restaurant.cart",
    "com.restaurant.order",
    "com.restaurant.payment",
    "com.restaurant.events",
    "com.restaurant.integration"
})
@EnableJpaRepositories(basePackages = {
    "com.restaurant.order.repository",
    "com.restaurant.payment.repository"
})
@EnableMongoRepositories(basePackages = {
    "com.restaurant.user.repository",
    "com.restaurant.restaurant.repository",
    "com.restaurant.cart.repository"
})
@EntityScan(basePackages = {
    "com.restaurant.order.domain",
    "com.restaurant.payment.domain"
})
@EnableKafka
public class IntegrationTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(IntegrationTestApplication.class, args);
    }
}