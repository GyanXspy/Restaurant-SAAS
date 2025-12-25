package com.restaurant.order.dlq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DLQ replay records.
 */
@Repository
public interface DlqReplayRepository extends JpaRepository<DlqReplayRecord, Long> {
    
    Optional<DlqReplayRecord> findByEventId(String eventId);
    
    List<DlqReplayRecord> findByStatus(DlqReplayStatus status);
    
    List<DlqReplayRecord> findByAggregateId(String aggregateId);
    
    boolean existsByEventId(String eventId);
}
