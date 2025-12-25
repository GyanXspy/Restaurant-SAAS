package com.restaurant.order.dlq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of a DLQ replay attempt.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DlqReplayResult {
    
    private String eventId;
    private boolean success;
    private String message;
    private DlqReplayStatus status;
}
