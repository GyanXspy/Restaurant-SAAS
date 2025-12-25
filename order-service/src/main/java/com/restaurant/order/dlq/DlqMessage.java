package com.restaurant.order.dlq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Represents a message in the Dead Letter Queue.
 * Contains the original event and failure metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DlqMessage {
    
    private String originalEvent;
    private String failureReason;
    private LocalDateTime failureTime;
    private String eventId;
    private String aggregateId;
    private String eventType;
}
