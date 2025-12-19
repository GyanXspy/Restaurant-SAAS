package com.restaurant.order.saga;

import com.restaurant.order.domain.OrderItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderSagaIntegrationTest {
    
    @Autowired
    private OrderSagaRepository sagaRepository;
    
    @Test
    void sagaRepository_ShouldSaveAndRetrieveSagaData() {
        // Given
        String orderId = "test-order-123";
        String customerId = "test-customer-456";
        String restaurantId = "test-restaurant-789";
        List<OrderItem> items = List.of(
            new OrderItem("item-1", "Test Pizza", new BigDecimal("15.99"), 1)
        );
        BigDecimal totalAmount = new BigDecimal("15.99");
        
        OrderSagaData sagaData = new OrderSagaData(orderId, customerId, restaurantId, items, totalAmount);
        sagaData.updateState(OrderSagaState.CART_VALIDATION_REQUESTED);
        
        // When
        OrderSagaData savedSaga = sagaRepository.save(sagaData);
        Optional<OrderSagaData> retrievedSaga = sagaRepository.findByOrderId(orderId);
        
        // Then
        assertNotNull(savedSaga);
        assertTrue(retrievedSaga.isPresent());
        
        OrderSagaData retrieved = retrievedSaga.get();
        assertEquals(orderId, retrieved.getOrderId());
        assertEquals(customerId, retrieved.getCustomerId());
        assertEquals(restaurantId, retrieved.getRestaurantId());
        assertEquals(totalAmount, retrieved.getTotalAmount());
        assertEquals(OrderSagaState.CART_VALIDATION_REQUESTED, retrieved.getState());
        assertEquals(1, retrieved.getItems().size());
        assertEquals("Test Pizza", retrieved.getItems().get(0).getName());
    }
    
    @Test
    void sagaRepository_ShouldFindSagasByState() {
        // Given
        OrderSagaData saga1 = new OrderSagaData("order-1", "customer-1", "restaurant-1", 
            List.of(new OrderItem("item-1", "Pizza", new BigDecimal("10.00"), 1)), new BigDecimal("10.00"));
        saga1.updateState(OrderSagaState.CART_VALIDATION_REQUESTED);
        
        OrderSagaData saga2 = new OrderSagaData("order-2", "customer-2", "restaurant-2", 
            List.of(new OrderItem("item-2", "Burger", new BigDecimal("12.00"), 1)), new BigDecimal("12.00"));
        saga2.updateState(OrderSagaState.PAYMENT_REQUESTED);
        
        sagaRepository.save(saga1);
        sagaRepository.save(saga2);
        
        // When
        List<OrderSagaData> cartValidationSagas = sagaRepository.findByState(OrderSagaState.CART_VALIDATION_REQUESTED);
        List<OrderSagaData> paymentSagas = sagaRepository.findByState(OrderSagaState.PAYMENT_REQUESTED);
        
        // Then
        assertEquals(1, cartValidationSagas.size());
        assertEquals("order-1", cartValidationSagas.get(0).getOrderId());
        
        assertEquals(1, paymentSagas.size());
        assertEquals("order-2", paymentSagas.get(0).getOrderId());
    }
}