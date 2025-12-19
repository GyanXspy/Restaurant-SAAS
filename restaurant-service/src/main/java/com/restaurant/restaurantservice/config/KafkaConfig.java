package com.restaurant.restaurantservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

import com.restaurant.events.publisher.DefaultTopicResolver;
import com.restaurant.events.publisher.EventPublisher;
import com.restaurant.events.publisher.KafkaEventPublisher;
import com.restaurant.events.publisher.TopicResolver;
import com.restaurant.events.serialization.EventSerializer;

/**
 * Kafka configuration for Restaurant Service.
 * Sets up event publishing infrastructure with topic resolution and serialization.
 */
@Configuration
public class KafkaConfig {

    @Value("${events.dead-letter-topic}")
    private String deadLetterTopic;

    @Bean
    public TopicResolver topicResolver() {
        return new DefaultTopicResolver();
    }

    @Bean
    public EventPublisher eventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            EventSerializer eventSerializer,
            TopicResolver topicResolver) {
        return new KafkaEventPublisher(kafkaTemplate, eventSerializer, topicResolver, deadLetterTopic);
    }
}