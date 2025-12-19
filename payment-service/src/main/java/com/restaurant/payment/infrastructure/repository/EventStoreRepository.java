package com.restaurant.payment.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.restaurant.payment.infrastructure.eventstore.EventStoreEntity;

@Repository
public interface EventStoreRepository extends JpaRepository<EventStoreEntity, Long> {

    @Query("SELECT e FROM EventStoreEntity e WHERE e.aggregateId = :aggregateId ORDER BY e.eventVersion ASC")
    List<EventStoreEntity> findByAggregateIdOrderByEventVersionAsc(@Param("aggregateId") String aggregateId);

    @Query("SELECT MAX(e.eventVersion) FROM EventStoreEntity e WHERE e.aggregateId = :aggregateId")
    Integer findMaxVersionByAggregateId(@Param("aggregateId") String aggregateId);
}