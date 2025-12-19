package com.restaurant.events.deadletter;

import com.restaurant.events.DomainEvent;
import com.restaurant.events.processing.EventProcessor;
import com.restaurant.events.processing.EventProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.LocalDateTime;

/**
 * Default implementation of DeadLetterQueueHandler.
 * Logs failed events and provides reprocessing capabilities.
 */
public class DefaultDeadLetterQueueHandler implements DeadLetterQueueHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultDeadLetterQueueHandler.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final EventProcessor eventProcessor;
    
    private static final String INSERT_FAILED_EVENT_SQL = 
        "INSERT INTO failed_events (event_id, event_type, event_data, failure_reason, attempt_count, failed_at) VALUES (?, ?, ?, ?, ?, ?)";
    
    private static final String UPDATE_FAILED_EVENT_SQL = 
        "UPDATE failed_events SET attempt_count = ?, failure_reason = ?, failed_at = ? WHERE event_id = ?";
    
    public DefaultDeadLetterQueueHandler(DataSource dataSource, EventProcessor eventProcessor) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.eventProcessor = eventProcessor;
    }
    
    @Override
    public void handleFailedEvent(DomainEvent failedEvent, String failureReason, int attemptCount) {
        try {
            logger.error("Handling failed event {} after {} attempts. Reason: {}", 
                failedEvent.getEventId(), attemptCount, failureReason);
            
            // Store the failed event for analysis and potential reprocessing
            storeFailedEvent(failedEvent, failureReason, attemptCount);
            
            // Send alert if this is a critical event or too many attempts
            if (attemptCount >= 3) {
                sendAlert(failedEvent, failureReason, attemptCount);
            }
            
        } catch (Exception e) {
            logger.error("Failed to handle dead letter event {}", failedEvent.getEventId(), e);
        }
    }
    
    @Override
    public boolean reprocessEvent(DomainEvent failedEvent) {
        try {
            logger.info("Attempting to reprocess failed event {}", failedEvent.getEventId());
            
            eventProcessor.process(failedEvent);
            
            logger.info("Successfully reprocessed failed event {}", failedEvent.getEventId());
            return true;
            
        } catch (EventProcessingException e) {
            logger.error("Failed to reprocess event {}", failedEvent.getEventId(), e);
            return false;
        }
    }
    
    private void storeFailedEvent(DomainEvent failedEvent, String failureReason, int attemptCount) {
        try {
            // Try to update existing record first
            int updated = jdbcTemplate.update(UPDATE_FAILED_EVENT_SQL,
                attemptCount, failureReason, LocalDateTime.now(), failedEvent.getEventId());
            
            if (updated == 0) {
                // Insert new record if update didn't affect any rows
                jdbcTemplate.update(INSERT_FAILED_EVENT_SQL,
                    failedEvent.getEventId(),
                    failedEvent.getEventType(),
                    failedEvent.toString(), // Store event as string for analysis
                    failureReason,
                    attemptCount,
                    LocalDateTime.now());
            }
            
        } catch (Exception e) {
            logger.error("Failed to store failed event {}", failedEvent.getEventId(), e);
        }
    }
    
    private void sendAlert(DomainEvent failedEvent, String failureReason, int attemptCount) {
        // In a real implementation, this would send alerts via email, Slack, etc.
        logger.error("ALERT: Critical event processing failure - Event: {}, Attempts: {}, Reason: {}", 
            failedEvent.getEventId(), attemptCount, failureReason);
    }
}