package com.restaurant.events.processing;

import com.restaurant.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

/**
 * Database-backed implementation of IdempotentEventProcessor.
 * Uses a database table to track processed events and ensure idempotency.
 */
public class DatabaseIdempotentEventProcessor implements IdempotentEventProcessor {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseIdempotentEventProcessor.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final EventProcessor eventProcessor;
    
    private static final String INSERT_PROCESSED_EVENT_SQL = 
        "INSERT INTO processed_events (event_id, processed_at) VALUES (?, NOW())";
    
    private static final String CHECK_EVENT_PROCESSED_SQL = 
        "SELECT COUNT(*) FROM processed_events WHERE event_id = ?";
    
    public DatabaseIdempotentEventProcessor(DataSource dataSource, EventProcessor eventProcessor) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.eventProcessor = eventProcessor;
    }
    
    @Override
    @Transactional
    public boolean processEvent(DomainEvent event) {
        String eventId = event.getEventId();
        
        try {
            // Try to mark the event as processed first
            markEventAsProcessed(eventId);
            
            // If successful, process the event
            logger.debug("Processing event {} for the first time", eventId);
            eventProcessor.process(event);
            
            logger.info("Successfully processed event {}", eventId);
            return true;
            
        } catch (DuplicateKeyException e) {
            // Event was already processed
            logger.debug("Event {} has already been processed, skipping", eventId);
            return false;
        } catch (Exception e) {
            logger.error("Failed to process event {}", eventId, e);
            throw new EventProcessingException("Failed to process event: " + eventId, e);
        }
    }
    
    @Override
    public boolean isEventProcessed(String eventId) {
        try {
            Integer count = jdbcTemplate.queryForObject(CHECK_EVENT_PROCESSED_SQL, 
                new Object[]{eventId}, 
                Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            logger.error("Failed to check if event {} was processed", eventId, e);
            throw new EventProcessingException("Failed to check event processing status: " + eventId, e);
        }
    }
    
    @Override
    public void markEventAsProcessed(String eventId) {
        try {
            jdbcTemplate.update(INSERT_PROCESSED_EVENT_SQL, eventId);
            logger.debug("Marked event {} as processed", eventId);
        } catch (DuplicateKeyException e) {
            // Event already marked as processed, this is expected in concurrent scenarios
            logger.debug("Event {} was already marked as processed", eventId);
        } catch (Exception e) {
            logger.error("Failed to mark event {} as processed", eventId, e);
            throw new EventProcessingException("Failed to mark event as processed: " + eventId, e);
        }
    }
}