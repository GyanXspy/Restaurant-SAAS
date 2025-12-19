package com.restaurant.integration;

import com.restaurant.events.*;
import com.restaurant.integration.testdata.TestDataBuilder;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end integration test for the complete order processing flow.
 * Tests the saga orchestration across all microservices.
 */
class OrderFlowIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EventCapture eventCapture;

    private String customerId;
    private String restaurantId;
    private String orderId;

    @BeforeEach
    void setUpTestData() throws InterruptedException {
        customerId = UUID.randomUUID().toString();
        restaurantId = UUID.randomUUID().toString();
        orderId = UUID.randomUUID().toString();

        eventCapture.clear();
        waitForServicesToBeReady();

        // Set up test data
        setupUserData();
        setupRestaurantData();
    }

    @Test
    void shouldProcessCompleteOrderFlowSuccessfully() throws InterruptedException {
        // Given: User and restaurant exist, cart is valid, payment succeeds
        
        // When: Order is created
        createOrder();

        // Then: Verify saga progression
        verifySagaProgression();
        
        // And: Verify final order state
        verifyOrderConfirmed();
    }

    @Test
    void shouldHandleCartValidationFailure() throws InterruptedException {
        // Given: Cart validation will fail
        setupCartValidationFailure();

        // When: Order is created
        createOrder();

        // Then: Verify saga compensation
        verifySagaCompensationForCartFailure();
    }

    @Test
    void shouldHandlePaymentFailure() throws InterruptedException {
        // Given: Payment will fail
        setupPaymentFailure();

        // When: Order is created
        createOrder();

        // Then: Verify saga compensation
        verifySagaCompensationForPaymentFailure();
    }

    @Test
    void shouldVerifyEventSourcingBehavior() throws InterruptedException {
        // Given: Order processing completes
        createOrder();
        waitForSagaCompletion();

        // When: Querying order events
        // Then: Verify event sourcing reconstruction
        verifyEventSourcingReconstruction();
    }

    @Test
    void shouldVerifyCQRSBehavior() throws InterruptedException {
        // Given: Order is created and processed
        createOrder();
        waitForSagaCompletion();

        // When: Querying through different models
        // Then: Verify CQRS read models are updated
        verifyCQRSReadModels();
    }

    private void setupUserData() {
        UserCreatedEvent userEvent = TestDataBuilder.UserTestData.createUserCreatedEvent(customerId);
        kafkaTemplate.send("user-created", userEvent);
    }

    private void setupRestaurantData() {
        RestaurantCreatedEvent restaurantEvent = TestDataBuilder.RestaurantTestData.createRestaurantCreatedEvent(restaurantId);
        kafkaTemplate.send("restaurant-created", restaurantEvent);

        MenuUpdatedEvent menuEvent = TestDataBuilder.RestaurantTestData.createMenuUpdatedEvent(restaurantId);
        kafkaTemplate.send("menu-updated", menuEvent);
    }

    private void setupCartValidationFailure() {
        // This would be handled by mocking the cart service response
        // For now, we'll simulate it in the test
    }

    private void setupPaymentFailure() {
        // This would be handled by mocking the payment service response
        // For now, we'll simulate it in the test
    }

    private void createOrder() {
        given()
            .contentType(ContentType.JSON)
            .body(createOrderRequest())
        .when()
            .post("/api/orders")
        .then()
            .statusCode(201)
            .body("orderId", notNullValue())
            .body("status", equalTo("PENDING"));
    }

    private Object createOrderRequest() {
        return new Object() {
            public final String customerId = OrderFlowIntegrationTest.this.customerId;
            public final String restaurantId = OrderFlowIntegrationTest.this.restaurantId;
            public final List<Object> items = List.of(
                new Object() {
                    public final String itemId = "item-1";
                    public final int quantity = 2;
                    public final BigDecimal price = new BigDecimal("12.99");
                },
                new Object() {
                    public final String itemId = "item-2";
                    public final int quantity = 1;
                    public final BigDecimal price = new BigDecimal("14.99");
                }
            );
            public final BigDecimal totalAmount = new BigDecimal("40.97");
        };
    }

    private void verifySagaProgression() {
        // Wait for and verify each saga step
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                assertTrue(eventCapture.hasEvent(OrderSagaStartedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationRequestedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationCompletedEvent.class));
                assertTrue(eventCapture.hasEvent(PaymentInitiationRequestedEvent.class));
                assertTrue(eventCapture.hasEvent(PaymentProcessingCompletedEvent.class));
                assertTrue(eventCapture.hasEvent(OrderConfirmedEvent.class));
            });
    }

    private void verifySagaCompensationForCartFailure() {
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                assertTrue(eventCapture.hasEvent(OrderSagaStartedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationRequestedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationCompletedEvent.class));
                assertTrue(eventCapture.hasEvent(OrderCancelledEvent.class));
                
                // Verify no payment was initiated
                assertFalse(eventCapture.hasEvent(PaymentInitiationRequestedEvent.class));
            });
    }

    private void verifySagaCompensationForPaymentFailure() {
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                assertTrue(eventCapture.hasEvent(OrderSagaStartedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationRequestedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationCompletedEvent.class));
                assertTrue(eventCapture.hasEvent(PaymentInitiationRequestedEvent.class));
                assertTrue(eventCapture.hasEvent(PaymentProcessingCompletedEvent.class));
                assertTrue(eventCapture.hasEvent(OrderCancelledEvent.class));
            });
    }

    private void verifyOrderConfirmed() {
        given()
        .when()
            .get("/api/orders/{orderId}", orderId)
        .then()
            .statusCode(200)
            .body("orderId", equalTo(orderId))
            .body("status", equalTo("CONFIRMED"))
            .body("customerId", equalTo(customerId))
            .body("restaurantId", equalTo(restaurantId));
    }

    private void waitForSagaCompletion() throws InterruptedException {
        Thread.sleep(5000); // Wait for saga to complete
    }

    private void verifyEventSourcingReconstruction() {
        // Verify that order can be reconstructed from events
        given()
        .when()
            .get("/api/orders/{orderId}/events", orderId)
        .then()
            .statusCode(200)
            .body("events", hasSize(greaterThan(0)))
            .body("events[0].eventType", notNullValue())
            .body("events[0].aggregateId", equalTo(orderId));
    }

    private void verifyCQRSReadModels() {
        // Verify command side
        given()
        .when()
            .get("/api/orders/{orderId}", orderId)
        .then()
            .statusCode(200)
            .body("orderId", equalTo(orderId));

        // Verify query side (read models)
        given()
        .when()
            .get("/api/orders/customer/{customerId}", customerId)
        .then()
            .statusCode(200)
            .body("orders", hasSize(greaterThan(0)))
            .body("orders[0].orderId", equalTo(orderId));
    }

    /**
     * Component to capture events for verification in tests
     */
    @Component
    static class EventCapture {
        private final List<DomainEvent> capturedEvents = new CopyOnWriteArrayList<>();

        @KafkaListener(topics = {
            "order-saga-started",
            "cart-validation-requested", 
            "cart-validation-completed",
            "payment-initiation-requested",
            "payment-processing-completed",
            "order-confirmed",
            "order-cancelled"
        })
        public void captureEvent(DomainEvent event) {
            capturedEvents.add(event);
        }

        public boolean hasEvent(Class<? extends DomainEvent> eventType) {
            return capturedEvents.stream()
                .anyMatch(event -> eventType.isAssignableFrom(event.getClass()));
        }

        public void clear() {
            capturedEvents.clear();
        }

        public List<DomainEvent> getCapturedEvents() {
            return List.copyOf(capturedEvents);
        }
    }
}