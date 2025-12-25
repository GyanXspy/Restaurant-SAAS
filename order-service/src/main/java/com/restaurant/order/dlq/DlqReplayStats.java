package com.restaurant.order.dlq;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Statistics for DLQ replay operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DlqReplayStats {
    
    private long pendingCount;
    private long replayedCount;
    private long failedCount;
    private long skippedCount;
    
    public long getTotalCount() {
        return pendingCount + replayedCount + failedCount + skippedCount;
    }
}
