package com.restaurant.order.dlq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.events.serialization.EventSerializer;
import com.restaurant.order.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for replaying failed events from DLQ.
 * Provides idempotent replay with audit trail.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DlqReplayService {
    
    private final DlqReplayRepository replayRepository;
    private final EventPublisher eventPublisher;
    private final EventSerializer eventSerializer;
    private final ObjectMapper objectMapper;
    
    /**
     * Replay a single event from DLQ by eventId.
     * Idempotent - won't replay if already replayed successfully.
     */
    @Transactional
    public DlqReplayResult replayEvent(String eventId) {
        log.info("Attempting to replay event: {}", eventId);
        
        // Check if already replayed
        DlqReplayRecord existingRecord = replayRepository.findByEventId(eventId).orElse(null);
        
        if (existingRecord != null && existingRecord.getStatus() == DlqReplayStatus.REPLAYED) {
            log.warn("Event {} already replayed successfully. Skipping.", eventId);
            return new DlqReplayResult(eventId, false, "Already replayed", DlqReplayStatus.SKIPPED);
        }
        
        if (existingRecord == null) {
            log.error("Event {} not found in DLQ replay records", eventId);
            return new DlqReplayResult(eventId, false, "Event not found in DLQ records", DlqReplayStatus.FAILED);
        }
        
        // Increment replay attempts
        existingRecord.setReplayAttempts(existingRecord.getReplayAttempts() + 1);
        existingRecord.setReplayAttemptTime(LocalDateTime.now());
        
        try {
            // Parse and deserialize the original event
            OrderEvent event = deserializeEvent(existingRecord.getOriginalEvent(), existingRecord.getEventType());
            
            // Republish to Kafka
            eventPublisher.publish(event);
            
            // Mark as replayed
            existingRecord.setStatus(DlqReplayStatus.REPLAYED);
            existingRecord.setReplayResult("Successfully replayed at " + LocalDateTime.now());
            replayRepository.save(existingRecord);
            
            log.info("Successfully replayed event: {}", eventId);
            return new DlqReplayResult(eventId, true, "Successfully replayed", DlqReplayStatus.REPLAYED);
            
        } catch (Exception e) {
            log.error("Failed to replay event: {}", eventId, e);
            
            existingRecord.setStatus(DlqReplayStatus.FAILED);
            existingRecord.setReplayResult("Replay failed: " + e.getMessage());
            replayRepository.save(existingRecord);
            
            return new DlqReplayResult(eventId, false, "Replay failed: " + e.getMessage(), DlqReplayStatus.FAILED);
        }
    }
    
    /**
     * Store DLQ message for future replay.
     */
    @Transactional
    public void storeDlqMessage(DlqMessage dlqMessage) {
        // Check if already exists (idempotent)
        if (replayRepository.existsByEventId(dlqMessage.getEventId())) {
            log.debug("DLQ message for event {} already stored", dlqMessage.getEventId());
            return;
        }
        
        DlqReplayRecord record = new DlqReplayRecord(
            dlqMessage.getEventId(),
            dlqMessage.getAggregateId(),
            dlqMessage.getEventType(),
            dlqMessage.getOriginalEvent(),
            dlqMessage.getFailureReason(),
            dlqMessage.getFailureTime()
        );
        
        replayRepository.save(record);
        log.info("Stored DLQ message for event: {}", dlqMessage.getEventId());
    }
    
    /**
     * Get replay statistics.
     */
    public DlqReplayStats getStats() {
        long pending = replayRepository.findByStatus(DlqReplayStatus.PENDING).size();
        long replayed = replayRepository.findByStatus(DlqReplayStatus.REPLAYED).size();
        long failed = replayRepository.findByStatus(DlqReplayStatus.FAILED).size();
        long skipped = replayRepository.findByStatus(DlqReplayStatus.SKIPPED).size();
        
        return new DlqReplayStats(pending, replayed, failed, skipped);
    }
    
    /**
     * Deserialize event from JSON based on event type.
     * Uses a custom ObjectMapper without type info to avoid subtype conflicts.
     */
    private OrderEvent deserializeEvent(String eventJson, String eventType) throws Exception {
        // Create ObjectMapper without type info handling
        ObjectMapper customMapper = new ObjectMapper();
        customMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        
        // Parse JSON and remove eventType field to avoid type resolution
        JsonNode jsonNode = customMapper.readTree(eventJson);
        
        // Deserialize based on event type
        return switch (eventType) {
            case "OrderCreated" -> customMapper.readValue(jsonNode.toString(), OrderCreatedEvent.class);
            case "OrderConfirmed" -> customMapper.readValue(jsonNode.toString(), OrderConfirmedEvent.class);
            case "OrderCancelled" -> customMapper.readValue(jsonNode.toString(), OrderCancelledEvent.class);
            default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
        };
    }
}
