package com.restaurant.order.infrastructure.resilience;

import com.restaurant.events.publisher.EventPublisher;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.apache.kafka.common.errors.RetriableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.event-publisher.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.event-publisher.wait-duration-in-open-state=1s",
    "resilience4j.circuitbreaker.instances.event-publisher.sliding-window-size=4",
    "resilience4j.circuitbreaker.instances.event-publisher.minimum-number-of-calls=2"
})
class ResilientEventPublisherTest {

    @Mock
    private EventPublisher mockDelegate;

    private ResilientEventPublisher resilientPublisher;
    private TestEvent testEvent;

    @BeforeEach
    void setUp() {
        resilientPublisher = new ResilientEventPublisher(mockDelegate);
        testEvent = new TestEvent("test-data");
    }

    @Test
    void shouldPublishEventSuccessfully() {
        // Given
        String topic = "test-topic";
        doNothing().when(mockDelegate).publish(topic, testEvent);

        // When
        resilientPublisher.publish(topic, testEvent);

        // Then
        verify(mockDelegate, times(1)).publish(topic, testEvent);
    }

    @Test
    void shouldPublishEventWithKeySuccessfully() {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        doNothing().when(mockDelegate).publish(topic, key, testEvent);

        // When
        resilientPublisher.publish(topic, key, testEvent);

        // Then
        verify(mockDelegate, times(1)).publish(topic, key, testEvent);
    }

    @Test
    void shouldRetryOnKafkaException() {
        // Given
        String topic = "test-topic";
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        doAnswer(invocation -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new KafkaException("Kafka connection error");
            }
            return null;
        }).when(mockDelegate).publish(eq(topic), any());

        // When
        resilientPublisher.publish(topic, testEvent);

        // Then
        verify(mockDelegate, times(3)).publish(topic, testEvent);
        assertEquals(3, attemptCount.get());
    }

    @Test
    void shouldRetryOnRetriableException() {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        doAnswer(invocation -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new RetriableException("Retriable Kafka error");
            }
            return null;
        }).when(mockDelegate).publish(eq(topic), eq(key), any());

        // When
        resilientPublisher.publish(topic, key, testEvent);

        // Then
        verify(mockDelegate, times(2)).publish(topic, key, testEvent);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void shouldRetryOnTimeoutException() {
        // Given
        String topic = "test-topic";
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        doAnswer(invocation -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("timeout occurred");
            }
            return null;
        }).when(mockDelegate).publish(eq(topic), any());

        // When
        resilientPublisher.publish(topic, testEvent);

        // Then
        verify(mockDelegate, times(3)).publish(topic, testEvent);
        assertEquals(3, attemptCount.get());
    }

    @Test
    void shouldFallbackAfterMaxRetries() {
        // Given
        String topic = "test-topic";
        doThrow(new KafkaException("Persistent Kafka error"))
            .when(mockDelegate).publish(eq(topic), any());

        // When & Then
        ResilientEventPublisher.EventPublishingException exception = 
            assertThrows(ResilientEventPublisher.EventPublishingException.class, () -> {
                resilientPublisher.publish(topic, testEvent);
            });

        assertTrue(exception.getMessage().contains("Event publishing temporarily unavailable"));
        assertTrue(exception.getMessage().contains(topic));
        assertNotNull(exception.getCause());
        verify(mockDelegate, atLeast(1)).publish(topic, testEvent);
    }

    @Test
    void shouldFallbackAfterMaxRetriesWithKey() {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        doThrow(new KafkaException("Persistent Kafka error"))
            .when(mockDelegate).publish(eq(topic), eq(key), any());

        // When & Then
        ResilientEventPublisher.EventPublishingException exception = 
            assertThrows(ResilientEventPublisher.EventPublishingException.class, () -> {
                resilientPublisher.publish(topic, key, testEvent);
            });

        assertTrue(exception.getMessage().contains("Event publishing temporarily unavailable"));
        assertTrue(exception.getMessage().contains(topic));
        assertTrue(exception.getMessage().contains(key));
        assertNotNull(exception.getCause());
        verify(mockDelegate, atLeast(1)).publish(topic, key, testEvent);
    }

    @Test
    void shouldHandleCircuitBreakerOpen() {
        // Given
        String topic = "test-topic";
        doThrow(new CallNotPermittedException("Circuit breaker is OPEN"))
            .when(mockDelegate).publish(eq(topic), any());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            resilientPublisher.publish(topic, testEvent);
        });

        assertTrue(exception instanceof CallNotPermittedException || 
                  exception instanceof ResilientEventPublisher.EventPublishingException);
    }

    @Test
    void shouldHandleBulkheadFull() {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        doThrow(new BulkheadFullException("Bulkhead is full"))
            .when(mockDelegate).publish(eq(topic), eq(key), any());

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            resilientPublisher.publish(topic, key, testEvent);
        });

        assertTrue(exception instanceof BulkheadFullException || 
                  exception instanceof ResilientEventPublisher.EventPublishingException);
    }

    @Test
    void shouldNotRetryOnNonRetriableException() {
        // Given
        String topic = "test-topic";
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        doAnswer(invocation -> {
            attemptCount.incrementAndGet();
            throw new IllegalArgumentException("Non-retriable error");
        }).when(mockDelegate).publish(eq(topic), any());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            resilientPublisher.publish(topic, testEvent);
        });

        assertEquals("Non-retriable error", exception.getMessage());
        assertEquals(1, attemptCount.get()); // Should not retry
    }

    @Test
    void shouldRetryOnNetworkException() {
        // Given
        String topic = "test-topic";
        AtomicInteger attemptCount = new AtomicInteger(0);
        
        doAnswer(invocation -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new RuntimeException("network connection failed");
            }
            return null;
        }).when(mockDelegate).publish(eq(topic), any());

        // When
        resilientPublisher.publish(topic, testEvent);

        // Then
        verify(mockDelegate, times(2)).publish(topic, testEvent);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void shouldPreserveFallbackBehaviorConsistency() {
        // Given
        KafkaException testException = new KafkaException("Test Kafka error");
        String topic = "test-topic";
        String key = "test-key";
        
        // When
        ResilientEventPublisher.EventPublishingException fallbackWithoutKey = 
            assertThrows(ResilientEventPublisher.EventPublishingException.class, () -> {
                resilientPublisher.fallbackPublish(topic, testEvent, testException);
            });
        
        ResilientEventPublisher.EventPublishingException fallbackWithKey = 
            assertThrows(ResilientEventPublisher.EventPublishingException.class, () -> {
                resilientPublisher.fallbackPublishWithKey(topic, key, testEvent, testException);
            });

        // Then
        assertTrue(fallbackWithoutKey.getMessage().contains("Event publishing temporarily unavailable"));
        assertTrue(fallbackWithoutKey.getMessage().contains(topic));
        assertFalse(fallbackWithoutKey.getMessage().contains("key:"));
        
        assertTrue(fallbackWithKey.getMessage().contains("Event publishing temporarily unavailable"));
        assertTrue(fallbackWithKey.getMessage().contains(topic));
        assertTrue(fallbackWithKey.getMessage().contains("key: " + key));
        
        assertEquals(testException, fallbackWithoutKey.getCause());
        assertEquals(testException, fallbackWithKey.getCause());
    }

    @Test
    void shouldHandleStoreFailedEventGracefully() {
        // Given
        String topic = "test-topic";
        KafkaException testException = new KafkaException("Test error");

        // When & Then - Should not throw additional exceptions during fallback
        ResilientEventPublisher.EventPublishingException exception = 
            assertThrows(ResilientEventPublisher.EventPublishingException.class, () -> {
                resilientPublisher.fallbackPublish(topic, testEvent, testException);
            });

        assertNotNull(exception);
        assertEquals(testException, exception.getCause());
    }

    // Test event class for testing
    private static class TestEvent {
        private final String data;

        public TestEvent(String data) {
            this.data = data;
        }

        public String getData() {
            return data;
        }

        @Override
        public String toString() {
            return "TestEvent{data='" + data + "'}";
        }
    }
}