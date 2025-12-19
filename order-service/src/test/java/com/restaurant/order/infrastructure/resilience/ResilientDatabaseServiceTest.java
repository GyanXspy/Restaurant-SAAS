package com.restaurant.order.infrastructure.resilience;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.TestPropertySource;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@TestPropertySource(properties = {
    "resilience4j.circuitbreaker.instances.database-read.failure-rate-threshold=50",
    "resilience4j.circuitbreaker.instances.database-read.wait-duration-in-open-state=1s",
    "resilience4j.circuitbreaker.instances.database-read.sliding-window-size=4",
    "resilience4j.circuitbreaker.instances.database-read.minimum-number-of-calls=2"
})
class ResilientDatabaseServiceTest {

    private ResilientDatabaseService resilientService;

    @BeforeEach
    void setUp() {
        resilientService = new ResilientDatabaseService();
    }

    @Test
    void shouldExecuteReadOperationSuccessfully() {
        // Given
        String expectedResult = "test-data";
        Supplier<String> operation = () -> expectedResult;

        // When
        String result = resilientService.executeRead(operation, "test-read");

        // Then
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldExecuteWriteOperationSuccessfully() {
        // Given
        String expectedResult = "write-success";
        Supplier<String> operation = () -> expectedResult;

        // When
        String result = resilientService.executeWrite(operation, "test-write");

        // Then
        assertEquals(expectedResult, result);
    }

    @Test
    void shouldExecuteVoidOperationSuccessfully() {
        // Given
        AtomicInteger counter = new AtomicInteger(0);
        Runnable operation = counter::incrementAndGet;

        // When
        resilientService.executeOperation(operation, "test-void-operation");

        // Then
        assertEquals(1, counter.get());
    }

    @Test
    void shouldRetryOnSQLException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException(new SQLException("Connection timeout"));
            }
            return "success-after-retry";
        };

        // When
        String result = resilientService.executeRead(operation, "test-retry");

        // Then
        assertEquals("success-after-retry", result);
        assertEquals(3, attemptCount.get());
    }

    @Test
    void shouldRetryOnDataAccessException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new DataAccessException("Database connection failed") {};
            }
            return "success-after-data-access-retry";
        };

        // When
        String result = resilientService.executeWrite(operation, "test-data-access-retry");

        // Then
        assertEquals("success-after-data-access-retry", result);
        assertEquals(2, attemptCount.get());
    }

    @Test
    void shouldFallbackAfterMaxRetriesForRead() {
        // Given
        Supplier<String> operation = () -> {
            throw new RuntimeException(new SQLException("Persistent database error"));
        };

        // When & Then
        ResilientDatabaseService.DatabaseUnavailableException exception = 
            assertThrows(ResilientDatabaseService.DatabaseUnavailableException.class, () -> {
                resilientService.executeRead(operation, "test-fallback-read");
            });

        assertTrue(exception.getMessage().contains("Database read operation temporarily unavailable"));
        assertTrue(exception.getMessage().contains("test-fallback-read"));
        assertNotNull(exception.getCause());
    }

    @Test
    void shouldFallbackAfterMaxRetriesForWrite() {
        // Given
        Supplier<String> operation = () -> {
            throw new RuntimeException(new SQLException("Persistent write error"));
        };

        // When & Then
        ResilientDatabaseService.DatabaseUnavailableException exception = 
            assertThrows(ResilientDatabaseService.DatabaseUnavailableException.class, () -> {
                resilientService.executeWrite(operation, "test-fallback-write");
            });

        assertTrue(exception.getMessage().contains("Database write operation temporarily unavailable"));
        assertTrue(exception.getMessage().contains("test-fallback-write"));
        assertNotNull(exception.getCause());
    }

    @Test
    void shouldFallbackForVoidOperation() {
        // Given
        Runnable operation = () -> {
            throw new RuntimeException(new SQLException("Void operation error"));
        };

        // When & Then
        ResilientDatabaseService.DatabaseUnavailableException exception = 
            assertThrows(ResilientDatabaseService.DatabaseUnavailableException.class, () -> {
                resilientService.executeOperation(operation, "test-fallback-void");
            });

        assertTrue(exception.getMessage().contains("Database operation temporarily unavailable"));
        assertTrue(exception.getMessage().contains("test-fallback-void"));
        assertNotNull(exception.getCause());
    }

    @Test
    void shouldHandleCircuitBreakerOpen() {
        // Given
        Supplier<String> operation = () -> {
            throw new CallNotPermittedException("Circuit breaker is OPEN");
        };

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            resilientService.executeRead(operation, "test-circuit-breaker");
        });

        assertTrue(exception instanceof CallNotPermittedException || 
                  exception instanceof ResilientDatabaseService.DatabaseUnavailableException);
    }

    @Test
    void shouldHandleBulkheadFull() {
        // Given
        Supplier<String> operation = () -> {
            throw new BulkheadFullException("Bulkhead is full");
        };

        // When & Then
        Exception exception = assertThrows(Exception.class, () -> {
            resilientService.executeWrite(operation, "test-bulkhead");
        });

        assertTrue(exception instanceof BulkheadFullException || 
                  exception instanceof ResilientDatabaseService.DatabaseUnavailableException);
    }

    @Test
    void shouldNotRetryOnNonRetriableException() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            attemptCount.incrementAndGet();
            throw new IllegalArgumentException("Non-retriable error");
        };

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            resilientService.executeRead(operation, "test-non-retriable");
        });

        assertEquals("Non-retriable error", exception.getMessage());
        assertEquals(1, attemptCount.get()); // Should not retry
    }

    @Test
    void shouldHandleConnectionTimeoutRetry() {
        // Given
        AtomicInteger attemptCount = new AtomicInteger(0);
        Supplier<String> operation = () -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("connection timeout");
            }
            return "success-after-timeout-retry";
        };

        // When
        String result = resilientService.executeRead(operation, "test-timeout-retry");

        // Then
        assertEquals("success-after-timeout-retry", result);
        assertEquals(3, attemptCount.get());
    }

    @Test
    void shouldPreserveFallbackBehaviorConsistency() {
        // Given
        SQLException testException = new SQLException("Test SQL error");
        RuntimeException wrappedException = new RuntimeException(testException);
        
        // When
        ResilientDatabaseService.DatabaseUnavailableException readFallback = 
            assertThrows(ResilientDatabaseService.DatabaseUnavailableException.class, () -> {
                resilientService.fallbackDatabaseRead(() -> "test", "test-read", wrappedException);
            });
        
        ResilientDatabaseService.DatabaseUnavailableException writeFallback = 
            assertThrows(ResilientDatabaseService.DatabaseUnavailableException.class, () -> {
                resilientService.fallbackDatabaseWrite(() -> "test", "test-write", wrappedException);
            });

        // Then
        assertTrue(readFallback.getMessage().contains("Database read operation temporarily unavailable"));
        assertTrue(writeFallback.getMessage().contains("Database write operation temporarily unavailable"));
        assertEquals(wrappedException, readFallback.getCause());
        assertEquals(wrappedException, writeFallback.getCause());
    }
}