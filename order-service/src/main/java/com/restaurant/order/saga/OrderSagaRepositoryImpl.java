package com.restaurant.order.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.order.domain.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * MySQL implementation of OrderSagaRepository.
 * Stores saga state in a dedicated table for persistence and recovery.
 */
@Repository
public class OrderSagaRepositoryImpl implements OrderSagaRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(OrderSagaRepositoryImpl.class);
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    
    public OrderSagaRepositoryImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public OrderSagaData save(OrderSagaData sagaData) {
        logger.debug("Saving saga data for order: {}", sagaData.getOrderId());
        
        try {
            String itemsJson = objectMapper.writeValueAsString(sagaData.getItems());
            
            String sql = """
                INSERT INTO order_saga_state (
                    order_id, customer_id, restaurant_id, items, total_amount, 
                    saga_state, payment_id, failure_reason, created_at, updated_at, retry_count
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    saga_state = VALUES(saga_state),
                    payment_id = VALUES(payment_id),
                    failure_reason = VALUES(failure_reason),
                    updated_at = VALUES(updated_at),
                    retry_count = VALUES(retry_count)
                """;
            
            jdbcTemplate.update(sql,
                sagaData.getOrderId(),
                sagaData.getCustomerId(),
                sagaData.getRestaurantId(),
                itemsJson,
                sagaData.getTotalAmount(),
                sagaData.getState().name(),
                sagaData.getPaymentId(),
                sagaData.getFailureReason(),
                Timestamp.valueOf(sagaData.getCreatedAt()),
                Timestamp.valueOf(sagaData.getUpdatedAt()),
                sagaData.getRetryCount()
            );
            
            logger.debug("Successfully saved saga data for order: {}", sagaData.getOrderId());
            return sagaData;
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize items for order: {}", sagaData.getOrderId(), e);
            throw new SagaRepositoryException("Failed to serialize saga data", e);
        } catch (Exception e) {
            logger.error("Failed to save saga data for order: {}", sagaData.getOrderId(), e);
            throw new SagaRepositoryException("Failed to save saga data", e);
        }
    }
    
    @Override
    public Optional<OrderSagaData> findByOrderId(String orderId) {
        logger.debug("Finding saga data for order: {}", orderId);
        
        try {
            String sql = """
                SELECT order_id, customer_id, restaurant_id, items, total_amount,
                       saga_state, payment_id, failure_reason, created_at, updated_at, retry_count
                FROM order_saga_state 
                WHERE order_id = ?
                """;
            
            List<OrderSagaData> results = jdbcTemplate.query(sql, new SagaDataRowMapper(), orderId);
            
            if (results.isEmpty()) {
                logger.debug("No saga data found for order: {}", orderId);
                return Optional.empty();
            }
            
            logger.debug("Found saga data for order: {}", orderId);
            return Optional.of(results.get(0));
            
        } catch (Exception e) {
            logger.error("Failed to find saga data for order: {}", orderId, e);
            throw new SagaRepositoryException("Failed to find saga data", e);
        }
    }
    
    @Override
    public List<OrderSagaData> findByState(OrderSagaState state) {
        logger.debug("Finding saga data by state: {}", state);
        
        try {
            String sql = """
                SELECT order_id, customer_id, restaurant_id, items, total_amount,
                       saga_state, payment_id, failure_reason, created_at, updated_at, retry_count
                FROM order_saga_state 
                WHERE saga_state = ?
                """;
            
            List<OrderSagaData> results = jdbcTemplate.query(sql, new SagaDataRowMapper(), state.name());
            logger.debug("Found {} saga data records in state: {}", results.size(), state);
            
            return results;
            
        } catch (Exception e) {
            logger.error("Failed to find saga data by state: {}", state, e);
            throw new SagaRepositoryException("Failed to find saga data by state", e);
        }
    }
    
    @Override
    public List<OrderSagaData> findInProgressSagas() {
        logger.debug("Finding in-progress saga data");
        
        try {
            String sql = """
                SELECT order_id, customer_id, restaurant_id, items, total_amount,
                       saga_state, payment_id, failure_reason, created_at, updated_at, retry_count
                FROM order_saga_state 
                WHERE saga_state NOT IN (?, ?)
                """;
            
            List<OrderSagaData> results = jdbcTemplate.query(sql, new SagaDataRowMapper(), 
                OrderSagaState.SAGA_COMPLETED.name(), OrderSagaState.SAGA_FAILED.name());
            
            logger.debug("Found {} in-progress saga data records", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Failed to find in-progress saga data", e);
            throw new SagaRepositoryException("Failed to find in-progress saga data", e);
        }
    }
    
    @Override
    public List<OrderSagaData> findTimedOutSagas(int timeoutMinutes) {
        logger.debug("Finding timed-out saga data with timeout: {} minutes", timeoutMinutes);
        
        try {
            String sql = """
                SELECT order_id, customer_id, restaurant_id, items, total_amount,
                       saga_state, payment_id, failure_reason, created_at, updated_at, retry_count
                FROM order_saga_state 
                WHERE saga_state NOT IN (?, ?) 
                AND updated_at < DATE_SUB(NOW(), INTERVAL ? MINUTE)
                """;
            
            List<OrderSagaData> results = jdbcTemplate.query(sql, new SagaDataRowMapper(), 
                OrderSagaState.SAGA_COMPLETED.name(), OrderSagaState.SAGA_FAILED.name(), timeoutMinutes);
            
            logger.debug("Found {} timed-out saga data records", results.size());
            return results;
            
        } catch (Exception e) {
            logger.error("Failed to find timed-out saga data", e);
            throw new SagaRepositoryException("Failed to find timed-out saga data", e);
        }
    }
    
    @Override
    public void deleteByOrderId(String orderId) {
        logger.debug("Deleting saga data for order: {}", orderId);
        
        try {
            String sql = "DELETE FROM order_saga_state WHERE order_id = ?";
            int deletedRows = jdbcTemplate.update(sql, orderId);
            
            if (deletedRows > 0) {
                logger.debug("Successfully deleted saga data for order: {}", orderId);
            } else {
                logger.debug("No saga data found to delete for order: {}", orderId);
            }
            
        } catch (Exception e) {
            logger.error("Failed to delete saga data for order: {}", orderId, e);
            throw new SagaRepositoryException("Failed to delete saga data", e);
        }
    }
    
    /**
     * Row mapper for converting database rows to OrderSagaData objects.
     */
    private class SagaDataRowMapper implements RowMapper<OrderSagaData> {
        
        @Override
        public OrderSagaData mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                OrderSagaData sagaData = new OrderSagaData();
                
                sagaData.setOrderId(rs.getString("order_id"));
                sagaData.setCustomerId(rs.getString("customer_id"));
                sagaData.setRestaurantId(rs.getString("restaurant_id"));
                sagaData.setTotalAmount(rs.getBigDecimal("total_amount"));
                sagaData.setState(OrderSagaState.valueOf(rs.getString("saga_state")));
                sagaData.setPaymentId(rs.getString("payment_id"));
                sagaData.setFailureReason(rs.getString("failure_reason"));
                sagaData.setRetryCount(rs.getInt("retry_count"));
                
                // Convert timestamps to LocalDateTime
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    sagaData.setCreatedAt(createdAt.toLocalDateTime());
                }
                
                Timestamp updatedAt = rs.getTimestamp("updated_at");
                if (updatedAt != null) {
                    sagaData.setUpdatedAt(updatedAt.toLocalDateTime());
                }
                
                // Deserialize items JSON
                String itemsJson = rs.getString("items");
                if (itemsJson != null && !itemsJson.trim().isEmpty()) {
                    OrderItem[] itemsArray = objectMapper.readValue(itemsJson, OrderItem[].class);
                    sagaData.setItems(List.of(itemsArray));
                }
                
                return sagaData;
                
            } catch (JsonProcessingException e) {
                logger.error("Failed to deserialize items JSON for order: {}", rs.getString("order_id"), e);
                throw new SQLException("Failed to deserialize saga data", e);
            }
        }
    }
}