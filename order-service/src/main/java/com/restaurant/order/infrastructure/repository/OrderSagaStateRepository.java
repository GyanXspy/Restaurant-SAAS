package com.restaurant.order.infrastructure.repository;

import com.restaurant.order.infrastructure.entity.OrderSagaStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderSagaStateRepository extends JpaRepository<OrderSagaStateEntity, String> {

    @Query("SELECT s FROM OrderSagaStateEntity s WHERE s.sagaState = :state ORDER BY s.createdAt ASC")
    List<OrderSagaStateEntity> findBySagaStateOrderByCreatedAtAsc(@Param("state") String state);

    @Query("SELECT s FROM OrderSagaStateEntity s WHERE s.sagaState IN :states AND s.updatedAt < :timeout")
    List<OrderSagaStateEntity> findTimeoutSagas(@Param("states") List<String> states, @Param("timeout") LocalDateTime timeout);

    @Query("SELECT s FROM OrderSagaStateEntity s WHERE s.retryCount < :maxRetries AND s.sagaState IN :retryableStates")
    List<OrderSagaStateEntity> findRetryableSagas(@Param("maxRetries") int maxRetries, @Param("retryableStates") List<String> retryableStates);
}