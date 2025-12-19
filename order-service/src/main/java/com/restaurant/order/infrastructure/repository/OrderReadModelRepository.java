package com.restaurant.order.infrastructure.repository;

import com.restaurant.order.infrastructure.entity.OrderReadModelEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderReadModelRepository extends JpaRepository<OrderReadModelEntity, String> {

    @Query("SELECT o FROM OrderReadModelEntity o WHERE o.customerId = :customerId ORDER BY o.createdAt DESC")
    Page<OrderReadModelEntity> findByCustomerIdOrderByCreatedAtDesc(@Param("customerId") String customerId, Pageable pageable);

    @Query("SELECT o FROM OrderReadModelEntity o WHERE o.restaurantId = :restaurantId ORDER BY o.createdAt DESC")
    Page<OrderReadModelEntity> findByRestaurantIdOrderByCreatedAtDesc(@Param("restaurantId") String restaurantId, Pageable pageable);

    @Query("SELECT o FROM OrderReadModelEntity o WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<OrderReadModelEntity> findByStatusOrderByCreatedAtDesc(@Param("status") String status);
}