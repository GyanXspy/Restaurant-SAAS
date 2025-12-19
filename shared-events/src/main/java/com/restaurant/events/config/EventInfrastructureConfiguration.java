package com.restaurant.events.config;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import com.restaurant.events.deadletter.DefaultDeadLetterQueueHandler;
import com.restaurant.events.processing.DatabaseIdempotentEventProcessor;
import com.restaurant.events.processing.EventProcessor;
import com.restaurant.events.publisher.DefaultTopicResolver;
import com.restaurant.events.publisher.KafkaEventPublisher;
import com.restaurant.events.serialization.EventSerializer;
import com.restaurant.events.store.MySqlEventStore;
import com.restaurant.events.versioning.EventSchemaRegistry;

/**
 * Configuration class for event infrastructure components. Provides default
 * beans that can be customized or overridden by applications.
 */
@Configuration
@ConditionalOnProperty(name = "restaurant.events.enabled", havingValue = "true", matchIfMissing = true)
public class EventInfrastructureConfiguration {

    @Bean
    public EventSerializer eventSerializer() {
        return new EventSerializer();
    }

    @Bean
    public EventSchemaRegistry eventSchemaRegistry() {
        return new EventSchemaRegistry();
    }

    @Bean
    public DefaultTopicResolver topicResolver() {
        return new DefaultTopicResolver();
    }

    @Bean
    @ConditionalOnProperty(name = "restaurant.events.store.type", havingValue = "mysql", matchIfMissing = true)
    public MySqlEventStore eventStore(DataSource dataSource, EventSerializer eventSerializer) {
        return new MySqlEventStore(dataSource, eventSerializer.getObjectMapper());
    }

    @Bean
    @ConditionalOnProperty(name = "restaurant.events.publisher.type", havingValue = "kafka", matchIfMissing = true)
    public KafkaEventPublisher eventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            EventSerializer eventSerializer,
            DefaultTopicResolver topicResolver) {

        String deadLetterTopic = "restaurant-events-dlq"; // Can be made configurable
        return new KafkaEventPublisher(kafkaTemplate, eventSerializer, topicResolver, deadLetterTopic);
    }

    @Bean
    @ConditionalOnProperty(name = "restaurant.events.idempotency.enabled", havingValue = "true", matchIfMissing = true)
    public DatabaseIdempotentEventProcessor idempotentEventProcessor(
            DataSource dataSource,
            EventProcessor eventProcessor) {
        return new DatabaseIdempotentEventProcessor(dataSource, eventProcessor);
    }

    @Bean
    public DefaultDeadLetterQueueHandler deadLetterQueueHandler(
            DataSource dataSource,
            EventProcessor eventProcessor) {
        return new DefaultDeadLetterQueueHandler(dataSource, eventProcessor);
    }
}
