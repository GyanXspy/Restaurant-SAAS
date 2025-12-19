package com.restaurant.order.saga;

import com.restaurant.events.*;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.order.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {
    
    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private OrderSagaRepository sagaRepository;
    
    private OrderSagaOrchestrator orchestrator;
    
    private String orderId;
    private String customerId;
    private String restaurantId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
    
    @BeforeEach
    void setUp() {
        orchestrator = new OrderSagaOrchestrator(eventPublisher, sagaRepository);
        
        orderId = "order-123";
        customerId = "customer-456";
        restaurantId = "restaurant-789";
        items = List.of(
            new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 2),
            new OrderItem("item-2", "Coke", new BigDecimal("2.50"), 1)
        );
        totalAmount = new BigDecimal("34.48");
    }
    
    @Test
    void startOrderSaga_ShouldCreateSagaDataAndPublishEvents() {
        // When
        orchestrator.startOrderSaga(orderId, customerId, restaurantId, items, totalAmount);
        
        // Then
        ArgumentCaptor<OrderSagaData> sagaCaptor = ArgumentCaptor.forClass(OrderSagaData.class);
        verify(sagaRepository).save(sagaCaptor.capture());
        
        OrderSagaData savedSaga = sagaCaptor.getValue();
        assertEquals(orderId, savedSaga.getOrderId());
        assertEquals(customerId, savedSaga.getCustomerId());
        assertEquals(restaurantId, savedSaga.getRestaurantId());
        assertEquals(totalAmount, savedSaga.getTotalAmount());
        assertEquals(OrderSagaState.CART_VALIDATION_REQUESTED, savedSaga.getState());
        
        // Verify events published
        verify(eventPublisher).publish(eq("order-saga-started"), any(OrderSagaStartedEvent.class));
        verify(eventPublisher).publish(eq("cart-validation-requested"), any(CartValidationRequestedEvent.class));
    }
    
    @Test
    void handleCartValidationCompleted_WhenValid_ShouldProceedToPayment() {
        // Given
        OrderSagaData sagaData = new OrderSagaData(orderId, customerId, restaurantId, items, totalAmount);
        sagaData.updateState(OrderSagaState.CART_VALIDATION_REQUESTED);
        
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.of(sagaData));
        
        CartValidationCompletedEvent event = new CartValidationCompletedEvent(
            orderId, "cart-123", orderId, true, null, 1
        );
        
        // When
        orchestrator.handleCartValidationCompleted(event);
        
        // Then
        ArgumentCaptor<OrderSagaData> sagaCaptor = ArgumentCaptor.forClass(OrderSagaData.class);
        verify(sagaRepository, times(2)).save(sagaCaptor.capture());
        
        List<OrderSagaData> savedSagas = sagaCaptor.getAllValues();
        assertEquals(OrderSagaState.CART_VALIDATED, savedSagas.get(0).getState());
        assertEquals(OrderSagaState.PAYMENT_REQUESTED, savedSagas.get(1).getState());
        
        verify(eventPublisher).publish(eq("payment-initiation-requested"), any(PaymentInitiationRequestedEvent.class));
    }
    
    @Test
    void handleCartValidationCompleted_WhenInvalid_ShouldCompensate() {
        // Given
        OrderSagaData sagaData = new OrderSagaData(orderId, customerId, restaurantId, items, totalAmount);
        sagaData.updateState(OrderSagaState.CART_VALIDATION_REQUESTED);
        
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.of(sagaData));
        
        CartValidationCompletedEvent event = new CartValidationCompletedEvent(
            orderId, "cart-123", orderId, false, List.of("Item not available"), 1
        );
        
        // When
        orchestrator.handleCartValidationCompleted(event);
        
        // Then
        ArgumentCaptor<OrderSagaData> sagaCaptor = ArgumentCaptor.forClass(OrderSagaData.class);
        verify(sagaRepository, times(2)).save(sagaCaptor.capture());
        
        List<OrderSagaData> savedSagas = sagaCaptor.getAllValues();
        assertEquals(OrderSagaState.CART_VALIDATION_FAILED, savedSagas.get(0).getState());
        assertEquals(OrderSagaState.SAGA_FAILED, savedSagas.get(1).getState());
        
        verify(eventPublisher).publish(eq("order-cancelled"), any(OrderCancelledEvent.class));
    }
    
    @Test
    void handlePaymentProcessingCompleted_WhenSuccessful_ShouldConfirmOrder() {
        // Given
        OrderSagaData sagaData = new OrderSagaData(orderId, customerId, restaurantId, items, totalAmount);
        sagaData.updateState(OrderSagaState.PAYMENT_REQUESTED);
        sagaData.setPaymentId("payment-123");
        
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.of(sagaData));
        
        PaymentProcessingCompletedEvent event = new PaymentProcessingCompletedEvent(
            orderId, "payment-123", orderId, totalAmount, 
            PaymentProcessingCompletedEvent.PaymentStatus.COMPLETED, null, 1
        );
        
        // When
        orchestrator.handlePaymentProcessingCompleted(event);
        
        // Then
        ArgumentCaptor<OrderSagaData> sagaCaptor = ArgumentCaptor.forClass(OrderSagaData.class);
        verify(sagaRepository, times(3)).save(sagaCaptor.capture());
        
        List<OrderSagaData> savedSagas = sagaCaptor.getAllValues();
        assertEquals(OrderSagaState.PAYMENT_COMPLETED, savedSagas.get(0).getState());
        assertEquals(OrderSagaState.ORDER_CONFIRMED, savedSagas.get(1).getState());
        assertEquals(OrderSagaState.SAGA_COMPLETED, savedSagas.get(2).getState());
        
        verify(eventPublisher).publish(eq("order-confirmed"), any(OrderConfirmedEvent.class));
    }
    
    @Test
    void handlePaymentProcessingCompleted_WhenFailed_ShouldCompensate() {
        // Given
        OrderSagaData sagaData = new OrderSagaData(orderId, customerId, restaurantId, items, totalAmount);
        sagaData.updateState(OrderSagaState.PAYMENT_REQUESTED);
        sagaData.setPaymentId("payment-123");
        
        when(sagaRepository.findByOrderId(orderId)).thenReturn(Optional.of(sagaData));
        
        PaymentProcessingCompletedEvent event = new PaymentProcessingCompletedEvent(
            orderId, "payment-123", orderId, totalAmount, 
            PaymentProcessingCompletedEvent.PaymentStatus.FAILED, "Insufficient funds", 1
        );
        
        // When
        orchestrator.handlePaymentProcessingCompleted(event);
        
        // Then
        ArgumentCaptor<OrderSagaData> sagaCaptor = ArgumentCaptor.forClass(OrderSagaData.class);
        verify(sagaRepository, times(2)).save(sagaCaptor.capture());
        
        List<OrderSagaData> savedSagas = sagaCaptor.getAllValues();
        assertEquals(OrderSagaState.PAYMENT_FAILED, savedSagas.get(0).getState());
        assertEquals(OrderSagaState.SAGA_FAILED, savedSagas.get(1).getState());
        
        verify(eventPublisher).publish(eq("order-cancelled"), any(OrderCancelledEvent.class));
    }
}