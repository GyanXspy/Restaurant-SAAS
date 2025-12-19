package com.restaurant.events.publisher;

import com.restaurant.events.DomainEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for publishing domain events to external systems.
 * Supports both synchronous and asynchronous event publishing.
 */
public interface EventPublisher {
    
    /**
     * Publishes a single domain event synchronously.
     * 
     * @param event the domain event to publish
     * @throws EventPublishingException if publishing fails
     */
    void publish(DomainEvent event);
    
    /**
     * Publishes a single domain event to a specific topic synchronously.
     * 
     * @param topic the target topic
     * @param event the domain event to publish
     * @throws EventPublishingException if publishing fails
     */
    void publish(String topic, DomainEvent event);
    
    /**
     * Publishes multiple domain events synchronously.
     * 
     * @param events the list of domain events to publish
     * @throws EventPublishingException if publishing fails
     */
    void publishAll(List<DomainEvent> events);
    
    /**
     * Publishes a single domain event asynchronously.
     * 
     * @param event the domain event to publish
     * @return CompletableFuture that completes when publishing is done
     */
    CompletableFuture<Void> publishAsync(DomainEvent event);
    
    /**
     * Publishes a single domain event to a specific topic asynchronously.
     * 
     * @param topic the target topic
     * @param event the domain event to publish
     * @return CompletableFuture that completes when publishing is done
     */
    CompletableFuture<Void> publishAsync(String topic, DomainEvent event);
    
    /**
     * Publishes multiple domain events asynchronously.
     * 
     * @param events the list of domain events to publish
     * @return CompletableFuture that completes when all events are published
     */
    CompletableFuture<Void> publishAllAsync(List<DomainEvent> events);
}