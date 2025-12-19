package com.restaurant.events.publisher;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.serialization.EventSerializer;

/**
 * Kafka implementation of EventPublisher with retry logic and dead letter queue
 * support. Provides reliable event publishing with configurable retry
 * mechanisms.
 */
public class KafkaEventPublisher implements EventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KafkaEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventSerializer eventSerializer;
    private final TopicResolver topicResolver;
    private final String deadLetterTopic;

    public KafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
            EventSerializer eventSerializer,
            TopicResolver topicResolver,
            String deadLetterTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventSerializer = eventSerializer;
        this.topicResolver = topicResolver;
        this.deadLetterTopic = deadLetterTopic;
    }

    @Override
    @Retryable(
            value = {EventPublishingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publish(DomainEvent event) {
        String topic = topicResolver.resolveTopicForEvent(event);
        publish(topic, event);
    }

    @Override
    @Retryable(
            value = {EventPublishingException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void publish(String topic, DomainEvent event) {
        try {
            String eventJson = eventSerializer.serialize(event);
            String key = event.getAggregateId(); // Use aggregate ID as partition key

            logger.debug("Publishing event {} to topic {} with key {}",
                    event.getEventId(), topic, key);

            CompletableFuture<SendResult<String, String>> future
                    = kafkaTemplate.send(topic, key, eventJson);

            // Wait for synchronous completion
            SendResult<String, String> result = future.get();

            logger.info("Successfully published event {} to topic {} at offset {}",
                    event.getEventId(), topic, result.getRecordMetadata().offset());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EventPublishingException("Event publishing was interrupted for event: " + event.getEventId(), e);
        } catch (ExecutionException e) {
            logger.error("Failed to publish event {} to topic {}", event.getEventId(), topic, e);
            handlePublishingFailure(event, e.getCause());
        } catch (Exception e) {
            logger.error("Unexpected error publishing event {} to topic {}", event.getEventId(), topic, e);
            throw new EventPublishingException("Failed to publish event: " + event.getEventId(), e);
        }
    }

    @Override
    public void publishAll(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            publish(event);
        }
    }

    @Override
    public CompletableFuture<Void> publishAsync(DomainEvent event) {
        String topic = topicResolver.resolveTopicForEvent(event);
        return publishAsync(topic, event);
    }

    @Override
    public CompletableFuture<Void> publishAsync(String topic, DomainEvent event) {
        try {
            String eventJson = eventSerializer.serialize(event);
            String key = event.getAggregateId();

            logger.debug("Publishing event {} asynchronously to topic {} with key {}",
                    event.getEventId(), topic, key);

            CompletableFuture<SendResult<String, String>> kafkaFuture
                    = kafkaTemplate.send(topic, key, eventJson);

            return kafkaFuture.handle((result, ex) -> {
                if (ex != null) {
                    logger.error("Failed to publish event {} to topic {}", event.getEventId(), topic, ex);
                    handlePublishingFailureAsync(event, ex);
                    throw new EventPublishingException("Failed to publish event: " + event.getEventId(), ex);
                } else {
                    logger.info("Successfully published event {} to topic {} at offset {}",
                            event.getEventId(), topic, result.getRecordMetadata().offset());
                    return null;
                }
            });

        } catch (Exception e) {
            logger.error("Unexpected error publishing event {} to topic {}", event.getEventId(), topic, e);
            return CompletableFuture.failedFuture(new EventPublishingException("Failed to publish event: " + event.getEventId(), e));
        }
    }

    @Override
    public CompletableFuture<Void> publishAllAsync(List<DomainEvent> events) {
        CompletableFuture<Void>[] futures = events.stream()
                .map(this::publishAsync)
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    /**
     * Handles publishing failures by sending events to dead letter queue.
     */
    private void handlePublishingFailure(DomainEvent event, Throwable cause) {
        try {
            logger.warn("Sending failed event {} to dead letter queue", event.getEventId());
            sendToDeadLetterQueue(event, cause);
        } catch (Exception dlqException) {
            logger.error("Failed to send event {} to dead letter queue", event.getEventId(), dlqException);
        }
        throw new EventPublishingException("Failed to publish event: " + event.getEventId(), cause);
    }

    /**
     * Handles async publishing failures by sending events to dead letter queue.
     */
    private void handlePublishingFailureAsync(DomainEvent event, Throwable cause) {
        try {
            logger.warn("Sending failed event {} to dead letter queue", event.getEventId());
            sendToDeadLetterQueue(event, cause);
        } catch (Exception dlqException) {
            logger.error("Failed to send event {} to dead letter queue", event.getEventId(), dlqException);
        }
    }

    /**
     * Sends failed events to dead letter queue for manual processing.
     */
    private void sendToDeadLetterQueue(DomainEvent event, Throwable cause) {
        try {
            String eventJson = eventSerializer.serialize(event);
            String key = event.getAggregateId();

            // Add failure metadata
            String dlqMessage = String.format("{\"originalEvent\":%s,\"failureReason\":\"%s\",\"failureTime\":\"%s\"}",
                    eventJson, cause.getMessage(), java.time.LocalDateTime.now());

            kafkaTemplate.send(deadLetterTopic, key, dlqMessage);
            logger.info("Sent failed event {} to dead letter queue", event.getEventId());

        } catch (Exception e) {
            logger.error("Failed to send event {} to dead letter queue", event.getEventId(), e);
            throw new EventPublishingException("Failed to send event to dead letter queue: " + event.getEventId(), e);
        }
    }
}
