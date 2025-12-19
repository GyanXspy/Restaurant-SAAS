package com.restaurant.payment.infrastructure.eventstore;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payment_events")
@Data
@NoArgsConstructor
public class EventStoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "event_data", nullable = false, columnDefinition = "JSON")
    private String eventData;

    @Column(name = "event_version", nullable = false)
    private Integer eventVersion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public EventStoreEntity(String aggregateId, String eventType, String eventData, Integer eventVersion) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.eventVersion = eventVersion;
        this.createdAt = LocalDateTime.now();
    }
}