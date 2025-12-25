package com.restaurant.order.eventstore;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Event Store entry for persisting domain events.
 * Stores events as JSON in MySQL for complete audit trail.
 */
@Entity
@Table(name = "event_store", indexes = {
    @Index(name = "idx_aggregate_id", columnList = "aggregateId"),
    @Index(name = "idx_event_type", columnList = "eventType"),
    @Index(name = "idx_occurred_on", columnList = "occurredOn")
})
@Data
@NoArgsConstructor
public class EventStoreEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String eventId;
    
    @Column(nullable = false)
    private String aggregateId; // orderId
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String eventData; // JSON
    
    @Column(nullable = false)
    private int version;
    
    @Column(nullable = false)
    private LocalDateTime occurredOn;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    public EventStoreEntry(String eventId, String aggregateId, String eventType, 
                          String eventData, int version, LocalDateTime occurredOn) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.version = version;
        this.occurredOn = occurredOn;
    }
}
