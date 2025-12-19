package com.restaurant.events.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.events.DomainEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * MySQL implementation of the EventStore interface.
 * Stores events in a MySQL database table with JSON serialization.
 */
public class MySqlEventStore implements EventStore {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String INSERT_EVENT_SQL = 
        "INSERT INTO events (aggregate_id, event_type, event_data, event_version, created_at) VALUES (?, ?, ?, ?, ?)";
    
    private static final String SELECT_EVENTS_BY_AGGREGATE_SQL = 
        "SELECT * FROM events WHERE aggregate_id = ? ORDER BY event_version ASC";
    
    private static final String SELECT_EVENTS_BY_AGGREGATE_FROM_VERSION_SQL = 
        "SELECT * FROM events WHERE aggregate_id = ? AND event_version >= ? ORDER BY event_version ASC";
    
    private static final String SELECT_EVENTS_BY_TYPE_SQL = 
        "SELECT * FROM events WHERE event_type = ? ORDER BY created_at ASC";
    
    private static final String SELECT_MAX_VERSION_SQL = 
        "SELECT COALESCE(MAX(event_version), 0) FROM events WHERE aggregate_id = ?";
    
    public MySqlEventStore(DataSource dataSource, ObjectMapper objectMapper) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.objectMapper = objectMapper;
    }
    
    @Override
    @Transactional
    public void saveEvent(DomainEvent event) {
        try {
            String eventData = objectMapper.writeValueAsString(event);
            jdbcTemplate.update(INSERT_EVENT_SQL,
                event.getAggregateId(),
                event.getEventType(),
                eventData,
                event.getVersion(),
                event.getOccurredOn()
            );
        } catch (JsonProcessingException e) {
            throw new EventStoreException("Failed to serialize event: " + event.getEventId(), e);
        } catch (Exception e) {
            throw new EventStoreException("Failed to save event: " + event.getEventId(), e);
        }
    }
    
    @Override
    @Transactional
    public void saveEvents(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            saveEvent(event);
        }
    }
    
    @Override
    public List<DomainEvent> getEventsForAggregate(String aggregateId) {
        try {
            return jdbcTemplate.query(SELECT_EVENTS_BY_AGGREGATE_SQL, 
                new Object[]{aggregateId}, 
                new EventRowMapper());
        } catch (Exception e) {
            throw new EventStoreException("Failed to retrieve events for aggregate: " + aggregateId, e);
        }
    }
    
    @Override
    public List<DomainEvent> getEventsForAggregateFromVersion(String aggregateId, int fromVersion) {
        try {
            return jdbcTemplate.query(SELECT_EVENTS_BY_AGGREGATE_FROM_VERSION_SQL,
                new Object[]{aggregateId, fromVersion},
                new EventRowMapper());
        } catch (Exception e) {
            throw new EventStoreException("Failed to retrieve events for aggregate: " + aggregateId + " from version: " + fromVersion, e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T extends DomainEvent> List<T> getEventsByType(Class<T> eventType) {
        try {
            String eventTypeName = getEventTypeName(eventType);
            List<DomainEvent> events = jdbcTemplate.query(SELECT_EVENTS_BY_TYPE_SQL,
                new Object[]{eventTypeName},
                new EventRowMapper());
            return (List<T>) events.stream()
                .filter(eventType::isInstance)
                .toList();
        } catch (Exception e) {
            throw new EventStoreException("Failed to retrieve events by type: " + eventType.getSimpleName(), e);
        }
    }
    
    @Override
    public int getCurrentVersion(String aggregateId) {
        try {
            Integer version = jdbcTemplate.queryForObject(SELECT_MAX_VERSION_SQL, 
                new Object[]{aggregateId}, 
                Integer.class);
            return version != null ? version : 0;
        } catch (Exception e) {
            throw new EventStoreException("Failed to get current version for aggregate: " + aggregateId, e);
        }
    }
    
    private String getEventTypeName(Class<? extends DomainEvent> eventType) {
        // Extract event type name from class name (remove "Event" suffix if present)
        String className = eventType.getSimpleName();
        return className.endsWith("Event") ? className.substring(0, className.length() - 5) : className;
    }
    
    private class EventRowMapper implements RowMapper<DomainEvent> {
        @Override
        public DomainEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                String eventData = rs.getString("event_data");
                return objectMapper.readValue(eventData, DomainEvent.class);
            } catch (JsonProcessingException e) {
                throw new SQLException("Failed to deserialize event data", e);
            }
        }
    }
}