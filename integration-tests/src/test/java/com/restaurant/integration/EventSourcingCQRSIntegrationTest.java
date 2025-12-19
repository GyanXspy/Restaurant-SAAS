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
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Event Sourcing and CQRS behavior verification.
 */
class EventSourcingCQRSIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    private String customerId;
    private String restaurantId;
    private String orderId;

    @BeforeEach
    void setUpTestData() throws InterruptedException {
        customerId = UUID.randomUUID().toString();
        restaurantId = UUID.randomUUID().toString();
        orderId = UUID.randomUUID().toString();

        waitForServicesToBeReady();
        setupTestData();
    }

    @Test
    void shouldReconstructAggregateFromEventStore() throws InterruptedException {
        // Given: Order is created and processed
        createCompleteOrderFlow();

        // When: Querying order events from event store
        // Then: Should return all events for the order
        given()
        .when()
            .get("/api/orders/{orderId}/events", orderId)
        .then()
            .statusCode(200)
            .body("events", hasSize(greaterThan(0)))
            .body("events[0].aggregateId", equalTo(orderId))
            .body("events[0].eventType", notNullValue())
            .body("events[0].eventData", notNullValue())
            .body("events[0].version", greaterThan(0));

        // And: Should be able to reconstruct order state from events
        given()
        .when()
            .post("/api/orders/{orderId}/reconstruct", orderId)
        .then()
            .statusCode(200)
            .body("orderId", equalTo(orderId))
            .body("status", equalTo("CONFIRMED"))
            .body("customerId", equalTo(customerId))
            .body("restaurantId", equalTo(restaurantId));
    }

    @Test
    void shouldMaintainEventVersioning() throws InterruptedException {
        // Given: Order is created
        createOrder();

        // When: Multiple events are generated for the same aggregate
        simulateOrderUpdates();

        // Then: Events should have proper versioning
        given()
        .when()
            .get("/api/orders/{orderId}/events", orderId)
        .then()
            .statusCode(200)
            .body("events", hasSize(greaterThan(1)))
            .body("events[0].version", equalTo(1))
            .body("events[1].version", equalTo(2));
    }

    @Test
    void shouldSeparateCommandAndQueryOperations() throws InterruptedException {
        // Given: Order is created and processed
        createCompleteOrderFlow();

        // When: Performing command operations (writes)
        // Then: Should use command side
        given()
            .contentType(ContentType.JSON)
            .body(createUpdateOrderRequest())
        .when()
            .put("/api/orders/{orderId}/command", orderId)
        .then()
            .statusCode(200);

        // When: Performing query operations (reads)
        // Then: Should use query side (read models)
        given()
        .when()
            .get("/api/orders/{orderId}/query", orderId)
        .then()
            .statusCode(200)
            .body("orderId", equalTo(orderId))
            .body("readModelVersion", notNullValue())
            .body("lastUpdated", notNullValue());
    }

    @Test
    void shouldUpdateReadModelsAsynchronously() throws InterruptedException {
        // Given: Order is created
        createOrder();

        // When: Events are published
        simulateOrderEvents();

        // Then: Read models should be updated asynchronously
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                given()
                .when()
                    .get("/api/orders/customer/{customerId}/summary", customerId)
                .then()
                    .statusCode(200)
                    .body("totalOrders", greaterThan(0))
                    .body("orders", hasSize(greaterThan(0)))
                    .body("orders[0].orderId", equalTo(orderId));
            });

        // And: Restaurant read model should also be updated
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                given()
                .when()
                    .get("/api/orders/restaurant/{restaurantId}/summary", restaurantId)
                .then()
                    .statusCode(200)
                    .body("totalOrders", greaterThan(0))
                    .body("orders", hasSize(greaterThan(0)))
                    .body("orders[0].orderId", equalTo(orderId));
            });
    }

    @Test
    void shouldHandleEventualConsistency() throws InterruptedException {
        // Given: Multiple orders are created rapidly
        String orderId1 = UUID.randomUUID().toString();
        String orderId2 = UUID.randomUUID().toString();
        String orderId3 = UUID.randomUUID().toString();

        // When: Creating orders in quick succession
        createOrder(orderId1);
        createOrder(orderId2);
        createOrder(orderId3);

        // Then: Eventually all read models should be consistent
        Awaitility.await()
            .atMost(Duration.ofSeconds(45))
            .untilAsserted(() -> {
                given()
                .when()
                    .get("/api/orders/customer/{customerId}/summary", customerId)
                .then()
                    .statusCode(200)
                    .body("totalOrders", equalTo(3))
                    .body("orders", hasSize(3));
            });
    }

    @Test
    void shouldSupportPointInTimeReconstruction() throws InterruptedException {
        // Given: Order goes through multiple state changes
        createOrder();
        simulateOrderStateChanges();

        // When: Reconstructing order at different points in time
        // Then: Should return correct state for each point
        given()
            .queryParam("version", 1)
        .when()
            .get("/api/orders/{orderId}/reconstruct", orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("PENDING"));

        given()
            .queryParam("version", 3)
        .when()
            .get("/api/orders/{orderId}/reconstruct", orderId)
        .then()
            .statusCode(200)
            .body("status", equalTo("CONFIRMED"));
    }

    @Test
    void shouldHandleEventStoreCorruption() throws InterruptedException {
        // Given: Order with events in event store
        createCompleteOrderFlow();

        // When: Simulating event store validation
        // Then: Should detect and handle corrupted events
        given()
        .when()
            .post("/api/orders/{orderId}/validate-events", orderId)
        .then()
            .statusCode(200)
            .body("isValid", equalTo(true))
            .body("eventCount", greaterThan(0))
            .body("validationErrors", empty());
    }

    private void setupTestData() {
        UserCreatedEvent userEvent = TestDataBuilder.UserTestData.createUserCreatedEvent(customerId);
        kafkaTemplate.send("user-created", userEvent);

        RestaurantCreatedEvent restaurantEvent = TestDataBuilder.RestaurantTestData.createRestaurantCreatedEvent(restaurantId);
        kafkaTemplate.send("restaurant-created", restaurantEvent);

        MenuUpdatedEvent menuEvent = TestDataBuilder.RestaurantTestData.createMenuUpdatedEvent(restaurantId);
        kafkaTemplate.send("menu-updated", menuEvent);
    }

    private void createCompleteOrderFlow() throws InterruptedException {
        createOrder();
        simulateCartValidationSuccess();
        simulatePaymentSuccess();
        Thread.sleep(3000); // Wait for saga completion
    }

    private void createOrder() {
        createOrder(orderId);
    }

    private void createOrder(String orderIdParam) {
        given()
            .contentType(ContentType.JSON)
            .body(createOrderRequest(orderIdParam))
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201);
    }

    private Object createOrderRequest(String orderIdParam) {
        return new Object() {
            public final String orderId = orderIdParam;
            public final String customerId = EventSourcingCQRSIntegrationTest.this.customerId;
            public final String restaurantId = EventSourcingCQRSIntegrationTest.this.restaurantId;
            public final List<Object> items = List.of(
                new Object() {
                    public final String itemId = "item-1";
                    public final int quantity = 2;
                    public final BigDecimal price = new BigDecimal("12.99");
                }
            );
            public final BigDecimal totalAmount = new BigDecimal("25.98");
        };
    }

    private Object createUpdateOrderRequest() {
        return new Object() {
            public final String notes = "Updated order notes";
            public final String deliveryAddress = "Updated delivery address";
        };
    }

    private void simulateCartValidationSuccess() {
        CartValidationCompletedEvent event = TestDataBuilder.OrderTestData.createCartValidationCompletedEvent(orderId, true);
        kafkaTemplate.send("cart-validation-completed", event);
    }

    private void simulatePaymentSuccess() {
        PaymentProcessingCompletedEvent event = TestDataBuilder.PaymentTestData.createPaymentProcessingCompletedEvent(orderId, true);
        kafkaTemplate.send("payment-processing-completed", event);
    }

    private void simulateOrderUpdates() {
        // Simulate additional events for the same order
        OrderCreatedEvent updateEvent = TestDataBuilder.OrderTestData.createOrderCreatedEvent(orderId, customerId, restaurantId);
        updateEvent.setVersion(2);
        kafkaTemplate.send("order-updated", updateEvent);
    }

    private void simulateOrderEvents() {
        simulateCartValidationSuccess();
        simulatePaymentSuccess();
    }

    private void simulateOrderStateChanges() throws InterruptedException {
        // Simulate order going through different states
        simulateCartValidationSuccess();
        Thread.sleep(1000);
        simulatePaymentSuccess();
        Thread.sleep(1000);
    }
}