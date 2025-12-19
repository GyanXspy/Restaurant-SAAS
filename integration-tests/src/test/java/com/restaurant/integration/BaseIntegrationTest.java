package com.restaurant.integration;

import com.restaurant.integration.config.TestContainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.config.RestAssuredConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Base class for integration tests providing common test infrastructure.
 * Sets up Testcontainers, Spring Boot context, and RestAssured configuration.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = IntegrationTestApplication.class
)
@Import(TestContainersConfiguration.class)
@Testcontainers
@ActiveProfiles("integration-test")
@TestPropertySource(properties = {
    "logging.level.com.restaurant=DEBUG",
    "spring.kafka.consumer.properties.spring.json.trusted.packages=com.restaurant.events",
    "spring.kafka.producer.properties.spring.json.type.mapping=*:com.restaurant.events.DomainEvent"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

    @LocalServerPort
    protected int port;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        
        // Configure ObjectMapper for RestAssured
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        RestAssured.config = RestAssuredConfig.config()
            .objectMapperConfig(ObjectMapperConfig.objectMapperConfig()
                .jackson2ObjectMapperFactory((cls, charset) -> objectMapper));
    }

    /**
     * Wait for Kafka topics to be created and services to be ready
     */
    protected void waitForServicesToBeReady() throws InterruptedException {
        Thread.sleep(2000); // Give services time to start up
    }

    /**
     * Clean up test data between tests
     */
    protected void cleanupTestData() {
        // This will be implemented by specific test classes as needed
    }
}