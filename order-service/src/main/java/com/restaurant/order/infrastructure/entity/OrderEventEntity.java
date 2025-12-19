package com.restaurant.order.infrastructure.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_events")
@Data
@NoArgsConstructor
public class OrderEventEntity {

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

    public OrderEventEntity(String aggregateId, String eventType, String eventData, Integer eventVersion) {
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.eventData = eventData;
        this.eventVersion = eventVersion;
        this.createdAt = LocalDateTime.now();
    }
}