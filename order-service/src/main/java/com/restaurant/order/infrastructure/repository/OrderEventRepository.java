package com.restaurant.order.infrastructure.repository;

import com.restaurant.order.infrastructure.entity.OrderEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderEventRepository extends JpaRepository<OrderEventEntity, Long> {

    @Query("SELECT e FROM OrderEventEntity e WHERE e.aggregateId = :aggregateId ORDER BY e.eventVersion ASC")
    List<OrderEventEntity> findByAggregateIdOrderByEventVersionAsc(@Param("aggregateId") String aggregateId);

    @Query("SELECT MAX(e.eventVersion) FROM OrderEventEntity e WHERE e.aggregateId = :aggregateId")
    Integer findMaxVersionByAggregateId(@Param("aggregateId") String aggregateId);
}