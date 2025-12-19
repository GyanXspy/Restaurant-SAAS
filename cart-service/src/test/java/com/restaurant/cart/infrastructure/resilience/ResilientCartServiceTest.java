package com.restaurant.cart.infrastructure.resilience;

import com.mongodb.MongoException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.mongodb.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.mongodb.wait-duration-in-open-state=1s",
    "resilience4j.circuitbreaker.instances.mongodb.sliding-window-size=4",
    "resilience4j.circuitbreaker.instances.mongodb.minimum-number-of-calls=2"
})
class ResilientCartServiceTest {

    private ResilientCartService resilientService;

    @BeforeEach
    void setUp() {
        resilientService = new ResilientCartService();
    }

    @Test
    void shouldExecuteMongoOperationSuccessfully() {
        // Given
        String expectedResult = "mongo-data";
        Supplier<String> operation = () -> expectedResult;

        // When
        String result = resilientService.executeMongoOperation(operation, "test-mongo-read");

        // Then
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldExecuteMongoVoidOperationSuccessfully() {
        // Given
        AtomicInteger counter = new AtomicInteger(0);
        Runnable operation = counter::incrementAndGet;

        // When
        resilientService.executeMongoVoidOperation(operation, "test-mongo-void");

        // Then
        assertEquals(1, counter.get());
    }

    @Test
    void shouldExecuteExternalServiceCallSuccessfully() {
        // Given
        String expectedResult = "external-service-data";
        Supplier<String> operation = () -> expectedResult;

        // When
        String result = resilientService.executeExternalServiceCall(operation, "restaurant-service");

        // Then
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldRetryOnMongoException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new MongoException("MongoDB connection timeout");
            }
            return "success-after-mongo-retry";
        };

        // When
        String result = resilientService.executeMongoOperation(operation, "test-mongo-retry");

        // Then
        assertEquals("success-after-mongo-retry", result);
        assertEquals(3, attemptCount.get());
    }

    @Test
    void shouldRetryOnDataAccessException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new DataAccessException("MongoDB data access failed") {};
            }
            return "success-after-data-access-retry";
        };

        // When
        String result = resilientService.executeMongoOperation(operation, "test-data-access-retry");

        // Then
        assertEquals("success-after-data-access-retry", result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void shouldRetryOnExternalServiceConnectionException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new ConnectException("Connection refused");
            }
            return "success-after-connection-retry";
        };

        // When
        String result = resilientService.executeExternalServiceCall(operation, "restaurant-service");

        // Then
        assertEquals("success-after-connection-retry", result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void shouldRetryOnSocketTimeoutException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new SocketTimeoutException("Socket timeout");
            }
            return "success-after-timeout-retry";
        };

        // When
        String result = resilientService.executeExternalServiceCall(operation, "restaurant-service");

        // Then
        assertEquals("success-after-timeout-retry", result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void shouldRetryOnResourceAccessException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new ResourceAccessException("Resource access failed");
            }
            return "success-after-resource-retry";
        };

        // When
        String result = resilientService.executeExternalServiceCall(operation, "restaurant-service");

        // Then
        assertEquals("success-after-resource-retry", result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void shouldFallbackAfterMaxRetriesForMongo() {
        // Given
        Supplier<String> operation = () -> {
            throw new MongoException("Persistent MongoDB error");
        };

        // When & Then
        ResilientCartService.MongoUnavailableException exception = 
            assertThrows(ResilientCartService.MongoUnavailableException.class, () -> {
                resilientService.executeMongoOperation(operation, "test-mongo-fallback");
            });

        assertTrue(exception.getMessage().contains("MongoDB operation temporarily unavailable"));
        assertTrue(exception.getMessage().contains("test-mongo-fallback"));
        assertNotNull(exception.getCause());
    }

    @Test
    void shouldFallbackForMongoVoidOperation() {
        // Given
        Runnable operation = () -> {
            throw new MongoException("MongoDB void operation error");
        };

        // When & Then
        ResilientCartService.MongoUnavailableException exception = 
            assertThrows(ResilientCartService.MongoUnavailableException.class, () -> {
                resilientService.executeMongoVoidOperation(operation, "test-mongo-void-fallback");
            });

        assertTrue(exception.getMessage().contains("MongoDB operation temporarily unavailable"));
        assertTrue(exception.getMessage().contains("test-mongo-void-fallback"));
        assertNotNull(exception.getCause());
    }

    @Test
    void shouldFallbackAfterMaxRetriesForExternalService() {
        // Given
        Supplier<String> operation = () -> {
            throw new ConnectException("Persistent connection error");
        };

        // When & Then
        ResilientCartService.ExternalServiceUnavailableException exception = 
            assertThrows(ResilientCartService.ExternalServiceUnavailableException.class, () -> {
                resilientService.executeExternalServiceCall(operation, "restaurant-service");
            });

        assertTrue(exception.getMessage().contains("External service temporarily unavailable"));
        assertTrue(exception.getMessage().contains("restaurant-service"));
        assertNotNull(exception.getCause());
    }

    @Test
    void shouldHandleCircuitBreakerOpenForMongo() {
        // Given
        Supplier<String> operation = () -> {
            throw new CallNotPermittedException("Circuit breaker is OPEN");
        };

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            resilientService.executeMongoOperation(operation, "test-circuit-breaker");
        });

        assertTrue(exception instanceof CallNotPermittedException || 
                  exception instanceof ResilientCartService.MongoUnavailableException);
    }

    @Test
    void shouldHandleBulkheadFullForExternalService() {
        // Given
        Supplier<String> operation = () -> {
            throw new BulkheadFullException("Bulkhead is full");
        };

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            resilientService.executeExternalServiceCall(operation, "restaurant-service");
        });

        assertTrue(exception instanceof BulkheadFullException || 
                  exception instanceof ResilientCartService.ExternalServiceUnavailableException);
    }

    @Test
    void shouldNotRetryOnNonRetriableExceptionForMongo() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw new IllegalArgumentException("Non-retriable MongoDB error");
        };

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            resilientService.executeMongoOperation(operation, "test-non-retriable-mongo");
        });

        assertEquals("Non-retriable MongoDB error", exception.getMessage());
        assertEquals(1, attemptCount.get()); // Should not retry
    }

    @Test
    void shouldNotRetryOnNonRetriableExceptionForExternalService() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw new IllegalStateException("Non-retriable external service error");
        };

        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            resilientService.executeExternalServiceCall(operation, "restaurant-service");
        });

        assertEquals("Non-retriable external service error", exception.getMessage());
        assertEquals(1, attemptCount.get()); // Should not retry
    }

    @Test
    void shouldPreserveFallbackBehaviorConsistency() {
        // Given
        MongoException mongoException = new MongoException("Test MongoDB error");
        ConnectException connectException = new ConnectException("Test connection error");
        
        // When
        ResilientCartService.MongoUnavailableException mongoFallback = 
            assertThrows(ResilientCartService.MongoUnavailableException.class, () -> {
                resilientService.fallbackMongoOperation(() -> "test", "test-mongo", mongoException);
            });
        
        ResilientCartService.ExternalServiceUnavailableException externalFallback = 
            assertThrows(ResilientCartService.ExternalServiceUnavailableException.class, () -> {
                resilientService.fallbackExternalService(() -> "test", "test-service", connectException);
            });

        // Then
        assertTrue(mongoFallback.getMessage().contains("MongoDB operation temporarily unavailable"));
        assertTrue(mongoFallback.getMessage().contains("test-mongo"));
        assertEquals(mongoException, mongoFallback.getCause());
        
        assertTrue(externalFallback.getMessage().contains("External service temporarily unavailable"));
        assertTrue(externalFallback.getMessage().contains("test-service"));
        assertEquals(connectException, externalFallback.getCause());
    }

    @Test
    void shouldHandleVoidOperationFallbackConsistency() {
        // Given
        MongoException testException = new MongoException("Test void operation error");
        
        // When & Then
        ResilientCartService.MongoUnavailableException exception = 
            assertThrows(ResilientCartService.MongoUnavailableException.class, () -> {
                resilientService.fallbackMongoVoidOperation(() -> {}, "test-void", testException);
            });

        assertTrue(exception.getMessage().contains("MongoDB operation temporarily unavailable"));
        assertTrue(exception.getMessage().contains("test-void"));
        assertEquals(testException, exception.getCause());
    }

    @Test
    void shouldDifferentiateBetweenMongoAndExternalServiceErrors() {
        // Given
        Supplier<String> mongoOperation = () -> {
            throw new MongoException("MongoDB specific error");
        };
        
        Supplier<String> externalOperation = () -> {
            throw new ConnectException("External service specific error");
        };

        // When & Then
        ResilientCartService.MongoUnavailableException mongoException = 
            assertThrows(ResilientCartService.MongoUnavailableException.class, () -> {
                resilientService.executeMongoOperation(mongoOperation, "mongo-test");
            });
        
        ResilientCartService.ExternalServiceUnavailableException externalException = 
            assertThrows(ResilientCartService.ExternalServiceUnavailableException.class, () -> {
                resilientService.executeExternalServiceCall(externalOperation, "external-test");
            });

        assertNotEquals(mongoException.getClass(), externalException.getClass());
        assertTrue(mongoException.getMessage().contains("MongoDB"));
        assertTrue(externalException.getMessage().contains("External service"));
    }
}