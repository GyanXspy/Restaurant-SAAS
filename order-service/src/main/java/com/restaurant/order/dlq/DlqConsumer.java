package com.restaurant.order.dlq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Kafka consumer for DLQ messages.
 * Consumes failed events and stores them for manual replay.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DlqConsumer {
    
    private final DlqReplayService replayService;
    private final ObjectMapper objectMapper;
    
    /**
     * Consume messages from DLQ topic and store for replay.
     */
    @KafkaListener(
        topics = "${kafka.topic.dead-letter:order-events-dlq}",
        groupId = "${spring.application.name}-dlq-consumer",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeDlqMessage(String message) {
        log.info("=== DLQ CONSUMER: Received message ===");
        log.info("DLQ Message: {}", message);
        
        try {
            // Parse DLQ message format: {"originalEvent":{...},"failureReason":"...","failureTime":"..."}
            JsonNode dlqNode = objectMapper.readTree(message);
            
            String originalEventJson = dlqNode.get("originalEvent").toString();
            String failureReason = dlqNode.get("failureReason").asText();
            String failureTimeStr = dlqNode.get("failureTime").asText();
            
            log.info("Parsed - Failure Reason: {}", failureReason);
            log.info("Parsed - Failure Time: {}", failureTimeStr);
            
            // Parse original event to extract metadata
            JsonNode originalEvent = objectMapper.readTree(originalEventJson);
            String eventId = originalEvent.get("eventId").asText();
            String aggregateId = originalEvent.get("aggregateId").asText();
            String eventType = originalEvent.get("eventType").asText();
            
            log.info("Event Details - ID: {}, Type: {}, AggregateID: {}", eventId, eventType, aggregateId);
            
            // Create DLQ message object
            DlqMessage dlqMessage = new DlqMessage(
                originalEventJson,
                failureReason,
                LocalDateTime.parse(failureTimeStr),
                eventId,
                aggregateId,
                eventType
            );
            
            // Store for manual replay
            replayService.storeDlqMessage(dlqMessage);
            
            log.info("=== DLQ CONSUMER: Successfully stored message for event: {} ===", eventId);
            
        } catch (Exception e) {
            log.error("=== DLQ CONSUMER: Failed to process message ===", e);
            log.error("Message content: {}", message);
        }
    }
}
