package com.restaurant.events.health;

import com.mongodb.client.MongoClient;
import org.bson.Document;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for MongoDB connectivity and performance.
 */
@Component
public class MongoHealthIndicator implements HealthIndicator {

    private final MongoTemplate mongoTemplate;

    public MongoHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Test basic connectivity with ping
            Document pingResult = mongoTemplate.getDb().runCommand(new Document("ping", 1));
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Get server status
            Document serverStatus = mongoTemplate.getDb().runCommand(new Document("serverStatus", 1));
            
            // Extract useful metrics
            Document connections = serverStatus.get("connections", Document.class);
            Document opcounters = serverStatus.get("opcounters", Document.class);
            
            int currentConnections = connections != null ? connections.getInteger("current", 0) : 0;
            int availableConnections = connections != null ? connections.getInteger("available", 0) : 0;
            int totalConnections = currentConnections + availableConnections;
            
            Health.Builder healthBuilder = Health.up()
                .withDetail("database", "MongoDB")
                .withDetail("responseTime", responseTime + "ms")
                .withDetail("currentConnections", currentConnections)
                .withDetail("availableConnections", availableConnections)
                .withDetail("totalConnections", totalConnections);
            
            if (connections != null) {
                double connectionUtilization = totalConnections > 0 ? 
                    (currentConnections * 100.0) / totalConnections : 0;
                healthBuilder.withDetail("connectionUtilization", 
                    String.format("%.2f%%", connectionUtilization));
                
                // Warn if connection utilization is high
                if (connectionUtilization > 80) {
                    healthBuilder.withDetail("warning", "High connection utilization");
                }
            }
            
            if (opcounters != null) {
                healthBuilder
                    .withDetail("operations.insert", opcounters.getLong("insert"))
                    .withDetail("operations.query", opcounters.getLong("query"))
                    .withDetail("operations.update", opcounters.getLong("update"))
                    .withDetail("operations.delete", opcounters.getLong("delete"));
            }
            
            // Warn if response time is high
            if (responseTime > 500) {
                healthBuilder.withDetail("warning", "High response time detected");
            }
            
            return healthBuilder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("database", "MongoDB")
                .withDetail("status", "Connection failed")
                .build();
        }
    }
}