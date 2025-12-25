package com.restaurant.order.dlq;

/**
 * Status of DLQ replay attempts.
 */
public enum DlqReplayStatus {
    PENDING,
    REPLAYED,
    FAILED,
    SKIPPED
}
