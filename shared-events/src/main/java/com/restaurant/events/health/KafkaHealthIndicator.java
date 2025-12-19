package com.restaurant.events.health;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Custom health indicator for Kafka connectivity and cluster health.
 */
@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try {
            AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties());
            
            DescribeClusterResult clusterResult = adminClient.describeCluster();
            String clusterId = clusterResult.clusterId().get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            int nodeCount = clusterResult.nodes().get(TIMEOUT.toSeconds(), TimeUnit.SECONDS).size();
            
            adminClient.close();
            
            return Health.up()
                .withDetail("clusterId", clusterId)
                .withDetail("nodeCount", nodeCount)
                .withDetail("status", "Connected")
                .build();
                
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .withDetail("status", "Disconnected")
                .build();
        }
    }
}