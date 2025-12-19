package com.restaurant.user.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.restaurant.events.publisher.DefaultTopicResolver;
import com.restaurant.events.serialization.EventSerializer;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.uri=mongodb://localhost:27017/test_user_service",
    "spring.kafka.bootstrap-servers=localhost:9092"
})
class KafkaConfigTest {

    @Test
    void testKafkaConfigurationBeans() {
        KafkaConfig config = new KafkaConfig();
        
        // Test EventSerializer bean creation
        EventSerializer eventSerializer = config.eventSerializer();
        assertNotNull(eventSerializer);
        
        // Test TopicResolver bean creation
        DefaultTopicResolver topicResolver = config.topicResolver();
        assertNotNull(topicResolver);
        
        // Note: KafkaTemplate and KafkaEventPublisher require Spring context
        // so they are tested in integration tests
    }
}