package com.restaurant.order.dlq;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Persists DLQ replay attempts for audit and idempotency.
 */
@Entity
@Table(name = "dlq_replay_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DlqReplayRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String eventId;
    
    @Column(nullable = false)
    private String aggregateId;
    
    @Column(nullable = false)
    private String eventType;
    
    @Column(columnDefinition = "TEXT")
    private String originalEvent;
    
    @Column(columnDefinition = "TEXT")
    private String failureReason;
    
    @Column(nullable = false)
    private LocalDateTime failureTime;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DlqReplayStatus status;
    
    @Column(nullable = false)
    private LocalDateTime replayAttemptTime;
    
    @Column(columnDefinition = "TEXT")
    private String replayResult;
    
    private Integer replayAttempts;
    
    public DlqReplayRecord(String eventId, String aggregateId, String eventType, 
                          String originalEvent, String failureReason, 
                          LocalDateTime failureTime) {
        this.eventId = eventId;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.originalEvent = originalEvent;
        this.failureReason = failureReason;
        this.failureTime = failureTime;
        this.status = DlqReplayStatus.PENDING;
        this.replayAttemptTime = LocalDateTime.now();
        this.replayAttempts = 0;
    }
}
