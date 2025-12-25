package com.restaurant.order.eventstore;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntry, Long> {
    
    List<EventStoreEntry> findByAggregateIdOrderByVersionAsc(String aggregateId);
    
    boolean existsByAggregateIdAndVersion(String aggregateId, int version);
}
