package com.restaurant.events.health;

import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Custom health indicator for database connectivity and performance.
 */
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Test basic connectivity
            String result = jdbcTemplate.queryForObject("SELECT 1", String.class);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Check connection pool status
            int activeConnections = getActiveConnections();
            int maxConnections = getMaxConnections();
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("database", "MySQL")
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("activeConnections", activeConnections)
                .withDetail("maxConnections", maxConnections)
                .withDetail("connectionUtilization", 
                    String.format("%.2f%%", (activeConnections * 100.0) / maxConnections));
            
            // Warn if response time is high
            if (responseTime > 1000) {
                healthBuilder.withDetail("warning", "High response time detected");
            }
            
            // Warn if connection pool is highly utilized
            if (activeConnections > maxConnections * 0.8) {
                healthBuilder.withDetail("warning", "High connection pool utilization");
            }
            
            return healthBuilder.build();
            
        } catch (DataAccessException e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("database", "MySQL")
                .withDetail("status", "Connection failed")
                .build();
        }
    }

    private int getActiveConnections() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.processlist WHERE db IS NOT NULL", 
                Integer.class);
        } catch (Exception e) {
            return -1;
        }
    }

    private int getMaxConnections() {
        try {
            return jdbcTemplate.queryForObject(
                "SELECT @@max_connections", 
                Integer.class);
        } catch (Exception e) {
            return 100; // Default fallback
        }
    }
}