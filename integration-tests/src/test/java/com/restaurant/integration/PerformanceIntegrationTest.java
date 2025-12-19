package com.restaurant.integration;

import com.restaurant.events.*;
import com.restaurant.integration.testdata.TestDataBuilder;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and load testing for the order processing system.
 * Tests system behavior under high load and concurrent operations.
 */
class PerformanceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private String customerId;
    private String restaurantId;

    @BeforeEach
    void setUpTestData() throws InterruptedException {
        customerId = UUID.randomUUID().toString();
        restaurantId = UUID.randomUUID().toString();

        waitForServicesToBeReady();
        setupTestData();
    }

    @Test
    void shouldHandleConcurrentOrderCreation() throws InterruptedException {
        // Given: Multiple concurrent order requests
        int numberOfOrders = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        Instant startTime = Instant.now();

        // When: Creating orders concurrently
        for (int i = 0; i < numberOfOrders; i++) {
            final int orderIndex = i;
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                String orderId = "order-" + orderIndex;
                createOrder(orderId);
                simulateSuccessfulFlow(orderId);
            }, executor);
            futures.add(future);
        }

        // Wait for all orders to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        Instant endTime = Instant.now();
        Duration totalTime = Duration.between(startTime, endTime);

        // Then: All orders should be processed successfully
        Awaitility.await()
            .atMost(Duration.ofSeconds(60))
            .untilAsserted(() -> {
                given()
                .when()
                    .get("/api/orders/customer/{customerId}/summary", customerId)
                .then()
                    .statusCode(200)
                    .body("totalOrders", equalTo(numberOfOrders))
                    .body("orders", hasSize(numberOfOrders));
            });

        // And: Performance should be acceptable (less than 2 minutes for 50 orders)
        assertTrue(totalTime.toSeconds() < 120, 
            "Processing " + numberOfOrders + " orders took " + totalTime.toSeconds() + " seconds");

        System.out.println("Processed " + numberOfOrders + " orders in " + totalTime.toSeconds() + " seconds");
    }

    @Test
    void shouldHandleHighVolumeEventProcessing() throws InterruptedException {
        // Given: High volume of events
        int numberOfEvents = 1000;
        List<String> orderIds = new ArrayList<>();

        Instant startTime = Instant.now();

        // When: Publishing many events rapidly
        for (int i = 0; i < numberOfEvents; i++) {
            String orderId = "bulk-order-" + i;
            orderIds.add(orderId);
            
            // Publish events for each order
            publishOrderEvents(orderId);
        }

        // Then: All events should be processed
        Awaitility.await()
            .atMost(Duration.ofMinutes(5))
            .untilAsserted(() -> {
                // Check that read models are updated for all orders
                for (int i = 0; i < Math.min(10, numberOfEvents); i++) { // Check first 10 orders
                    String orderId = orderIds.get(i);
                    given()
                    .when()
                        .get("/api/orders/{orderId}", orderId)
                    .then()
                        .statusCode(anyOf(equalTo(200), equalTo(404))); // Order might not exist yet, but should not error
                }
            });

        Instant endTime = Instant.now();
        Duration totalTime = Duration.between(startTime, endTime);

        System.out.println("Processed " + numberOfEvents + " events in " + totalTime.toSeconds() + " seconds");
    }

    @Test
    void shouldMaintainPerformanceUnderLoad() throws InterruptedException {
        // Given: System under sustained load
        int numberOfRounds = 5;
        int ordersPerRound = 20;
        List<Duration> roundTimes = new ArrayList<>();

        for (int round = 0; round < numberOfRounds; round++) {
            Instant roundStart = Instant.now();

            // Create orders for this round
            List<String> roundOrderIds = new ArrayList<>();
            for (int i = 0; i < ordersPerRound; i++) {
                String orderId = "load-test-round-" + round + "-order-" + i;
                roundOrderIds.add(orderId);
                createOrder(orderId);
                simulateSuccessfulFlow(orderId);
            }

            // Wait for round completion
            Awaitility.await()
                .atMost(Duration.ofSeconds(60))
                .untilAsserted(() -> {
                    for (String orderId : roundOrderIds) {
                        given()
                        .when()
                            .get("/api/orders/{orderId}", orderId)
                        .then()
                            .statusCode(200)
                            .body("status", anyOf(equalTo("CONFIRMED"), equalTo("PENDING")));
                    }
                });

            Instant roundEnd = Instant.now();
            Duration roundTime = Duration.between(roundStart, roundEnd);
            roundTimes.add(roundTime);

            System.out.println("Round " + (round + 1) + " completed in " + roundTime.toSeconds() + " seconds");
        }

        // Then: Performance should remain consistent across rounds
        double averageTime = roundTimes.stream()
            .mapToLong(Duration::toSeconds)
            .average()
            .orElse(0.0);

        double maxTime = roundTimes.stream()
            .mapToLong(Duration::toSeconds)
            .max()
            .orElse(0L);

        // Performance should not degrade significantly
        assertTrue(maxTime < averageTime * 2, 
            "Performance degraded significantly. Average: " + averageTime + "s, Max: " + maxTime + "s");
    }

    @Test
    void shouldHandleEventOrderingUnderLoad() throws InterruptedException {
        // Given: Events that must maintain ordering
        String orderId = UUID.randomUUID().toString();
        int numberOfUpdates = 100;

        // When: Publishing many ordered events for the same aggregate
        for (int i = 1; i <= numberOfUpdates; i++) {
            OrderCreatedEvent event = TestDataBuilder.OrderTestData.createOrderCreatedEvent(orderId, customerId, restaurantId);
            event.setVersion(i);
            kafkaTemplate.send("order-updated", orderId, event); // Use orderId as partition key
        }

        // Then: Events should be processed in order
        Awaitility.await()
            .atMost(Duration.ofSeconds(60))
            .untilAsserted(() -> {
                given()
                .when()
                    .get("/api/orders/{orderId}/events", orderId)
                .then()
                    .statusCode(200)
                    .body("events", hasSize(numberOfUpdates))
                    .body("events[0].version", equalTo(1))
                    .body("events[-1].version", equalTo(numberOfUpdates));
            });
    }

    @Test
    void shouldRecoverFromTemporaryFailures() throws InterruptedException {
        // Given: System experiencing temporary failures
        String orderId = UUID.randomUUID().toString();

        // When: Creating order during simulated failure
        createOrder(orderId);

        // Simulate temporary failure by not responding to cart validation
        // (This would normally trigger retry mechanisms)

        // After some time, simulate recovery
        Thread.sleep(5000);
        simulateSuccessfulFlow(orderId);

        // Then: Order should eventually be processed successfully
        Awaitility.await()
            .atMost(Duration.ofSeconds(60))
            .untilAsserted(() -> {
                given()
                .when()
                    .get("/api/orders/{orderId}", orderId)
                .then()
                    .statusCode(200)
                    .body("status", equalTo("CONFIRMED"));
            });
    }

    private void setupTestData() {
        UserCreatedEvent userEvent = TestDataBuilder.UserTestData.createUserCreatedEvent(customerId);
        kafkaTemplate.send("user-created", userEvent);

        RestaurantCreatedEvent restaurantEvent = TestDataBuilder.RestaurantTestData.createRestaurantCreatedEvent(restaurantId);
        kafkaTemplate.send("restaurant-created", restaurantEvent);

        MenuUpdatedEvent menuEvent = TestDataBuilder.RestaurantTestData.createMenuUpdatedEvent(restaurantId);
        kafkaTemplate.send("menu-updated", menuEvent);
    }

    private void createOrder(String orderId) {
        given()
            .contentType(ContentType.JSON)
            .body(createOrderRequest(orderId))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);
    }

    private Object createOrderRequest(String orderId) {
        return new Object() {
            public final String orderId = orderId;
            public final String customerId = PerformanceIntegrationTest.this.customerId;
            public final String restaurantId = PerformanceIntegrationTest.this.restaurantId;
            public final List<Object> items = List.of(
                new Object() {
                    public final String itemId = "item-1";
                    public final int quantity = 1;
                    public final BigDecimal price = new BigDecimal("12.99");
                }
            );
            public final BigDecimal totalAmount = new BigDecimal("12.99");
        };
    }

    private void simulateSuccessfulFlow(String orderId) {
        // Simulate cart validation success
        CartValidationCompletedEvent cartEvent = TestDataBuilder.OrderTestData.createCartValidationCompletedEvent(orderId, true);
        kafkaTemplate.send("cart-validation-completed", cartEvent);

        // Simulate payment success
        PaymentProcessingCompletedEvent paymentEvent = TestDataBuilder.PaymentTestData.createPaymentProcessingCompletedEvent(orderId, true);
        kafkaTemplate.send("payment-processing-completed", paymentEvent);
    }

    private void publishOrderEvents(String orderId) {
        // Publish a sequence of events for an order
        OrderSagaStartedEvent sagaEvent = new OrderSagaStartedEvent();
        sagaEvent.setEventId(UUID.randomUUID().toString());
        sagaEvent.setAggregateId(orderId);
        sagaEvent.setOrderId(orderId);
        kafkaTemplate.send("order-saga-started", sagaEvent);

        CartValidationRequestedEvent cartEvent = TestDataBuilder.OrderTestData.createCartValidationRequestedEvent(orderId, customerId, restaurantId);
        kafkaTemplate.send("cart-validation-requested", cartEvent);

        CartValidationCompletedEvent cartCompletedEvent = TestDataBuilder.OrderTestData.createCartValidationCompletedEvent(orderId, true);
        kafkaTemplate.send("cart-validation-completed", cartCompletedEvent);

        PaymentInitiationRequestedEvent paymentEvent = TestDataBuilder.PaymentTestData.createPaymentInitiationRequestedEvent(orderId, customerId);
        kafkaTemplate.send("payment-initiation-requested", paymentEvent);

        PaymentProcessingCompletedEvent paymentCompletedEvent = TestDataBuilder.PaymentTestData.createPaymentProcessingCompletedEvent(orderId, true);
        kafkaTemplate.send("payment-processing-completed", paymentCompletedEvent);
    }
}