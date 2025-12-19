package com.restaurant.integration;

import com.restaurant.events.*;
import com.restaurant.integration.testdata.TestDataBuilder;
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
 * Integration tests focused on saga compensation scenarios and failure handling.
 */
class SagaCompensationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private CompensationEventCapture eventCapture;

    private String customerId;
    private String restaurantId;

    @BeforeEach
    void setUpTestData() throws InterruptedException {
        customerId = UUID.randomUUID().toString();
        restaurantId = UUID.randomUUID().toString();

        eventCapture.clear();
        waitForServicesToBeReady();

        setupTestData();
    }

    @Test
    void shouldCompensateWhenCartValidationFails() throws InterruptedException {
        // Given: Cart validation will fail due to unavailable items
        String orderId = UUID.randomUUID().toString();

        // When: Order is created
        createOrder(orderId);

        // And: Cart validation fails
        simulateCartValidationFailure(orderId);

        // Then: Saga should compensate
        Awaitility.await()
            .atMost(Duration.ofSeconds(30))
            .untilAsserted(() -> {
                assertTrue(eventCapture.hasEvent(OrderSagaStartedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationRequestedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationCompletedEvent.class));
                assertTrue(eventCapture.hasEvent(OrderCancelledEvent.class));
                
                // Verify compensation - no payment should be initiated
                assertFalse(eventCapture.hasEvent(PaymentInitiationRequestedEvent.class));
            });

        // And: Order status should be cancelled
        verifyOrderCancelled(orderId);
    }

    @Test
    void shouldCompensateWhenPaymentFails() throws InterruptedException {
        // Given: Payment will fail
        String orderId = UUID.randomUUID().toString();

        // When: Order is created
        createOrder(orderId);

        // And: Cart validation succeeds
        simulateCartValidationSuccess(orderId);

        // And: Payment fails
        simulatePaymentFailure(orderId);

        // Then: Saga should compensate
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

        // And: Order status should be cancelled
        verifyOrderCancelled(orderId);
    }

    @Test
    void shouldHandleTimeoutInCartValidation() throws InterruptedException {
        // Given: Cart validation will timeout
        String orderId = UUID.randomUUID().toString();

        // When: Order is created
        createOrder(orderId);

        // And: No cart validation response is received (timeout scenario)
        // Don't send any cart validation response

        // Then: Saga should timeout and compensate
        Awaitility.await()
            .atMost(Duration.ofSeconds(60)) // Longer timeout for saga timeout handling
            .untilAsserted(() -> {
                assertTrue(eventCapture.hasEvent(OrderSagaStartedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationRequestedEvent.class));
                assertTrue(eventCapture.hasEvent(OrderCancelledEvent.class));
            });
    }

    @Test
    void shouldHandleTimeoutInPaymentProcessing() throws InterruptedException {
        // Given: Payment processing will timeout
        String orderId = UUID.randomUUID().toString();

        // When: Order is created and cart validation succeeds
        createOrder(orderId);
        simulateCartValidationSuccess(orderId);

        // And: No payment response is received (timeout scenario)
        // Don't send any payment response

        // Then: Saga should timeout and compensate
        Awaitility.await()
            .atMost(Duration.ofSeconds(60)) // Longer timeout for saga timeout handling
            .untilAsserted(() -> {
                assertTrue(eventCapture.hasEvent(OrderSagaStartedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationRequestedEvent.class));
                assertTrue(eventCapture.hasEvent(CartValidationCompletedEvent.class));
                assertTrue(eventCapture.hasEvent(PaymentInitiationRequestedEvent.class));
                assertTrue(eventCapture.hasEvent(OrderCancelledEvent.class));
            });
    }

    @Test
    void shouldHandleMultipleFailureScenarios() throws InterruptedException {
        // Test multiple orders with different failure scenarios
        String orderId1 = UUID.randomUUID().toString();
        String orderId2 = UUID.randomUUID().toString();
        String orderId3 = UUID.randomUUID().toString();

        // Scenario 1: Cart validation failure
        createOrder(orderId1);
        simulateCartValidationFailure(orderId1);

        // Scenario 2: Payment failure
        createOrder(orderId2);
        simulateCartValidationSuccess(orderId2);
        simulatePaymentFailure(orderId2);

        // Scenario 3: Successful order
        createOrder(orderId3);
        simulateCartValidationSuccess(orderId3);
        simulatePaymentSuccess(orderId3);

        // Verify all scenarios handled correctly
        Awaitility.await()
            .atMost(Duration.ofSeconds(45))
            .untilAsserted(() -> {
                // Should have 3 saga starts
                assertEquals(3, eventCapture.countEvents(OrderSagaStartedEvent.class));
                
                // Should have 2 cancellations and 1 confirmation
                assertEquals(2, eventCapture.countEvents(OrderCancelledEvent.class));
                assertEquals(1, eventCapture.countEvents(OrderConfirmedEvent.class));
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
            public final String customerId = SagaCompensationIntegrationTest.this.customerId;
            public final String restaurantId = SagaCompensationIntegrationTest.this.restaurantId;
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

    private void simulateCartValidationFailure(String orderId) {
        CartValidationCompletedEvent event = TestDataBuilder.OrderTestData.createCartValidationCompletedEvent(orderId, false);
        kafkaTemplate.send("cart-validation-completed", event);
    }

    private void simulateCartValidationSuccess(String orderId) {
        CartValidationCompletedEvent event = TestDataBuilder.OrderTestData.createCartValidationCompletedEvent(orderId, true);
        kafkaTemplate.send("cart-validation-completed", event);
    }

    private void simulatePaymentFailure(String orderId) {
        PaymentProcessingCompletedEvent event = TestDataBuilder.PaymentTestData.createPaymentProcessingCompletedEvent(orderId, false);
        kafkaTemplate.send("payment-processing-completed", event);
    }

    private void simulatePaymentSuccess(String orderId) {
        PaymentProcessingCompletedEvent event = TestDataBuilder.PaymentTestData.createPaymentProcessingCompletedEvent(orderId, true);
        kafkaTemplate.send("payment-processing-completed", event);
    }

    private void verifyOrderCancelled(String orderId) {
        given()
        .when()
            .get("/api/orders/{orderId}", orderId)
        .then()
            .statusCode(200)
            .body("orderId", equalTo(orderId))
            .body("status", equalTo("CANCELLED"));
    }

    /**
     * Enhanced event capture for compensation testing
     */
    @Component
    static class CompensationEventCapture {
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

        public long countEvents(Class<? extends DomainEvent> eventType) {
            return capturedEvents.stream()
                .filter(event -> eventType.isAssignableFrom(event.getClass()))
                .count();
        }

        public void clear() {
            capturedEvents.clear();
        }

        public List<DomainEvent> getCapturedEvents() {
            return List.copyOf(capturedEvents);
        }
    }
}