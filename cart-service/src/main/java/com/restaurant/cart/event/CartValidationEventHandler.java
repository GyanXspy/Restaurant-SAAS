package com.restaurant.cart.event;

import com.restaurant.cart.service.CartService;
import com.restaurant.events.CartValidationCompletedEvent;
import com.restaurant.events.CartValidationRequestedEvent;
import com.restaurant.events.publisher.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Event handler for cart validation requests from the order saga.
 */
@Component
public class CartValidationEventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CartValidationEventHandler.class);

    private final CartService cartService;
    private final EventPublisher eventPublisher;

    @Autowired
    public CartValidationEventHandler(CartService cartService, EventPublisher eventPublisher) {
        this.cartService = cartService;
        this.eventPublisher = eventPublisher;
    }

    @KafkaListener(
        topics = "cart-validation-requested",
        containerFactory = "cartValidationListenerContainerFactory"
    )
    public void handleCartValidationRequested(
            @Payload CartValidationRequestedEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        logger.info("Received cart validation request for cartId: {}, orderId: {}, customerId: {}", 
                   event.getCartId(), event.getOrderId(), event.getCustomerId());
        
        try {
            // Perform cart validation
            List<String> validationErrors = cartService.validateCart(event.getCartId());
            boolean isValid = validationErrors.isEmpty();
            
            // Create and publish cart validation completed event
            CartValidationCompletedEvent completedEvent = new CartValidationCompletedEvent(
                event.getAggregateId(), // Use saga ID as aggregate ID
                event.getCartId(),
                event.getOrderId(),
                isValid,
                validationErrors,
                1
            );
            
            eventPublisher.publish(completedEvent);
            
            if (isValid) {
                logger.info("Cart validation successful for cartId: {}, orderId: {}", 
                           event.getCartId(), event.getOrderId());
                
                // Mark cart as checked out if validation is successful
                cartService.markCartAsCheckedOut(event.getCartId());
            } else {
                logger.warn("Cart validation failed for cartId: {}, orderId: {}, errors: {}", 
                           event.getCartId(), event.getOrderId(), validationErrors);
            }
            
            // Acknowledge the message after successful processing
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing cart validation request for cartId: {}", event.getCartId(), e);
            
            // Publish validation failed event
            try {
                CartValidationCompletedEvent failedEvent = new CartValidationCompletedEvent(
                    event.getAggregateId(),
                    event.getCartId(),
                    event.getOrderId(),
                    false,
                    List.of("Internal error during cart validation: " + e.getMessage()),
                    1
                );
                eventPublisher.publish(failedEvent);
            } catch (Exception publishException) {
                logger.error("Failed to publish cart validation failure event", publishException);
            }
            
            // Don't acknowledge - message will be retried
            throw e;
        }
    }
}