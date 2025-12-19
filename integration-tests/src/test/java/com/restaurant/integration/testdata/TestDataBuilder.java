package com.restaurant.integration.testdata;

import com.restaurant.events.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Builder class for creating consistent test data across integration tests.
 */
public class TestDataBuilder {

    public static class UserTestData {
        public static UserCreatedEvent createUserCreatedEvent() {
            return createUserCreatedEvent(UUID.randomUUID().toString());
        }

        public static UserCreatedEvent createUserCreatedEvent(String userId) {
            UserCreatedEvent event = new UserCreatedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(userId);
            event.setOccurredOn(LocalDateTime.now());
            event.setVersion(1);
            event.setUserId(userId);
            event.setEmail("test.user@example.com");
            event.setFirstName("Test");
            event.setLastName("User");
            return event;
        }
    }

    public static class RestaurantTestData {
        public static RestaurantCreatedEvent createRestaurantCreatedEvent() {
            return createRestaurantCreatedEvent(UUID.randomUUID().toString());
        }

        public static RestaurantCreatedEvent createRestaurantCreatedEvent(String restaurantId) {
            RestaurantCreatedEvent event = new RestaurantCreatedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(restaurantId);
            event.setOccurredOn(LocalDateTime.now());
            event.setVersion(1);
            event.setRestaurantId(restaurantId);
            event.setName("Test Restaurant");
            event.setCuisine("Italian");
            event.setAddress("123 Test Street, Test City");
            return event;
        }

        public static MenuUpdatedEvent createMenuUpdatedEvent(String restaurantId) {
            MenuUpdatedEvent event = new MenuUpdatedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(restaurantId);
            event.setOccurredOn(LocalDateTime.now());
            event.setVersion(1);
            event.setRestaurantId(restaurantId);
            event.setMenuItems(List.of(
                createMenuItem("item-1", "Pizza Margherita", new BigDecimal("12.99")),
                createMenuItem("item-2", "Pasta Carbonara", new BigDecimal("14.99"))
            ));
            return event;
        }

        private static MenuUpdatedEvent.MenuItem createMenuItem(String itemId, String name, BigDecimal price) {
            MenuUpdatedEvent.MenuItem item = new MenuUpdatedEvent.MenuItem();
            item.setItemId(itemId);
            item.setName(name);
            item.setPrice(price);
            item.setCategory("Main Course");
            item.setAvailable(true);
            return item;
        }
    }

    public static class OrderTestData {
        public static OrderCreatedEvent createOrderCreatedEvent() {
            return createOrderCreatedEvent(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
            );
        }

        public static OrderCreatedEvent createOrderCreatedEvent(String orderId, String customerId, String restaurantId) {
            OrderCreatedEvent event = new OrderCreatedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(orderId);
            event.setOccurredOn(LocalDateTime.now());
            event.setVersion(1);
            event.setOrderId(orderId);
            event.setCustomerId(customerId);
            event.setRestaurantId(restaurantId);
            event.setItems(List.of(
                createOrderItem("item-1", "Pizza Margherita", 2, new BigDecimal("12.99")),
                createOrderItem("item-2", "Pasta Carbonara", 1, new BigDecimal("14.99"))
            ));
            event.setTotalAmount(new BigDecimal("40.97"));
            return event;
        }

        private static OrderCreatedEvent.OrderItem createOrderItem(String itemId, String name, int quantity, BigDecimal price) {
            OrderCreatedEvent.OrderItem item = new OrderCreatedEvent.OrderItem();
            item.setItemId(itemId);
            item.setName(name);
            item.setQuantity(quantity);
            item.setPrice(price);
            return item;
        }

        public static CartValidationRequestedEvent createCartValidationRequestedEvent(String orderId, String customerId, String restaurantId) {
            CartValidationRequestedEvent event = new CartValidationRequestedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(orderId);
            event.setOccurredOn(LocalDateTime.now());
            event.setVersion(1);
            event.setOrderId(orderId);
            event.setCustomerId(customerId);
            event.setRestaurantId(restaurantId);
            event.setItems(List.of(
                createCartItem("item-1", 2, new BigDecimal("12.99")),
                createCartItem("item-2", 1, new BigDecimal("14.99"))
            ));
            return event;
        }

        private static CartValidationRequestedEvent.CartItem createCartItem(String itemId, int quantity, BigDecimal price) {
            CartValidationRequestedEvent.CartItem item = new CartValidationRequestedEvent.CartItem();
            item.setItemId(itemId);
            item.setQuantity(quantity);
            item.setPrice(price);
            return item;
        }

        public static CartValidationCompletedEvent createCartValidationCompletedEvent(String orderId, boolean isValid) {
            CartValidationCompletedEvent event = new CartValidationCompletedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(orderId);
            event.setOccurredOn(LocalDateTime.now());
            event.setVersion(1);
            event.setOrderId(orderId);
            event.setIsValid(isValid);
            if (!isValid) {
                event.setValidationErrors(List.of("Item item-1 is out of stock"));
            }
            return event;
        }
    }

    public static class PaymentTestData {
        public static PaymentInitiationRequestedEvent createPaymentInitiationRequestedEvent(String orderId, String customerId) {
            PaymentInitiationRequestedEvent event = new PaymentInitiationRequestedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(orderId);
            event.setOccurredOn(LocalDateTime.now());
            event.setVersion(1);
            event.setOrderId(orderId);
            event.setCustomerId(customerId);
            event.setAmount(new BigDecimal("40.97"));
            event.setPaymentMethod("CREDIT_CARD");
            return event;
        }

        public static PaymentProcessingCompletedEvent createPaymentProcessingCompletedEvent(String orderId, boolean isSuccessful) {
            PaymentProcessingCompletedEvent event = new PaymentProcessingCompletedEvent();
            event.setEventId(UUID.randomUUID().toString());
            event.setAggregateId(orderId);
            event.setOccurredOn(LocalDateTime.now());
            event.setVersion(1);
            event.setOrderId(orderId);
            event.setPaymentId(UUID.randomUUID().toString());
            event.setIsSuccessful(isSuccessful);
            event.setAmount(new BigDecimal("40.97"));
            if (!isSuccessful) {
                event.setFailureReason("Insufficient funds");
            }
            return event;
        }
    }
}