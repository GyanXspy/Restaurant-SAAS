package com.restaurant.order.dlq;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for DLQ replay operations.
 * Backend-only endpoints for future UI integration.
 */
@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DLQ Replay", description = "Dead Letter Queue replay operations")
public class DlqReplayController {
    
    private final DlqReplayService replayService;
    private final DlqReplayRepository replayRepository;
    
    /**
     * Replay a single event from DLQ.
     * POST /api/dlq/replay/{eventId}
     */
    @PostMapping("/replay/{eventId}")
    @Operation(summary = "Replay a failed event from DLQ")
    public ResponseEntity<DlqReplayResult> replayEvent(@PathVariable String eventId) {
        log.info("REST API: Replay request for event: {}", eventId);
        
        DlqReplayResult result = replayService.replayEvent(eventId);
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
    
    /**
     * Get DLQ replay statistics.
     * GET /api/dlq/stats
     */
    @GetMapping("/stats")
    @Operation(summary = "Get DLQ replay statistics")
    public ResponseEntity<DlqReplayStats> getStats() {
        log.info("REST API: Get DLQ stats");
        return ResponseEntity.ok(replayService.getStats());
    }
    
    /**
     * Get all pending DLQ messages.
     * GET /api/dlq/pending
     */
    @GetMapping("/pending")
    @Operation(summary = "Get all pending DLQ messages")
    public ResponseEntity<List<DlqReplayRecord>> getPendingMessages() {
        log.info("REST API: Get pending DLQ messages");
        List<DlqReplayRecord> pending = replayRepository.findByStatus(DlqReplayStatus.PENDING);
        return ResponseEntity.ok(pending);
    }
    
    /**
     * Get DLQ messages by status.
     * GET /api/dlq/status/{status}
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get DLQ messages by status")
    public ResponseEntity<List<DlqReplayRecord>> getMessagesByStatus(@PathVariable DlqReplayStatus status) {
        log.info("REST API: Get DLQ messages with status: {}", status);
        List<DlqReplayRecord> messages = replayRepository.findByStatus(status);
        return ResponseEntity.ok(messages);
    }
    
    /**
     * Get DLQ message details by eventId.
     * GET /api/dlq/event/{eventId}
     */
    @GetMapping("/event/{eventId}")
    @Operation(summary = "Get DLQ message details")
    public ResponseEntity<DlqReplayRecord> getEventDetails(@PathVariable String eventId) {
        log.info("REST API: Get DLQ message details for event: {}", eventId);
        return replayRepository.findByEventId(eventId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Reset a failed event to PENDING status for retry.
     * PUT /api/dlq/reset/{eventId}
     */
    @PutMapping("/reset/{eventId}")
    @Operation(summary = "Reset a failed event to PENDING status")
    public ResponseEntity<DlqReplayRecord> resetEvent(@PathVariable String eventId) {
        log.info("REST API: Reset event to PENDING: {}", eventId);
        
        return replayRepository.findByEventId(eventId)
            .map(record -> {
                record.setStatus(DlqReplayStatus.PENDING);
                record.setReplayResult(null);
                DlqReplayRecord updated = replayRepository.save(record);
                log.info("Event {} reset to PENDING", eventId);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}
