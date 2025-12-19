# Resilience Patterns Implementation

This document describes the resilience patterns implemented in the Restaurant Food Ordering System using Resilience4j to ensure high availability and fault tolerance.

## Overview

The system implements four key resilience patterns:
1. **Circuit Breaker** - Prevents cascade failures by stopping calls to failing services
2. **Bulkhead** - Isolates resources to prevent resource exhaustion
3. **Retry** - Automatically retries failed operations with exponential backoff
4. **Time Limiter** - Prevents operations from running indefinitely

## Implementation Details

### 1. Circuit Breaker Pattern

Circuit breakers are implemented for:
- **Payment Gateway** (Payment Service)
- **Database Operations** (Order Service)
- **Event Publishing** (Order Service)
- **MongoDB Operations** (Cart Service)
- **External Service Calls** (Cart Service)

#### Configuration Example (Payment Gateway)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      payment-gateway:
        failure-rate-threshold: 50          # Open circuit at 50% failure rate
        wait-duration-in-open-state: 30s    # Wait 30s before trying again
        sliding-window-size: 10             # Consider last 10 calls
        minimum-number-of-calls: 5          # Need 5 calls before calculating failure rate
        permitted-number-of-calls-in-half-open-state: 3  # Allow 3 calls in half-open
        slow-call-rate-threshold: 50        # Consider slow calls as failures
        slow-call-duration-threshold: 5s    # Calls >5s are slow
        register-health-indicator: true     # Expose health metrics
```

#### States
- **CLOSED**: Normal operation, calls pass through
- **OPEN**: Circuit is open, calls fail fast with fallback
- **HALF_OPEN**: Testing if service has recovered

### 2. Bulkhead Pattern

Bulkheads limit concurrent resource usage to prevent resource exhaustion:

#### Resource Isolation
- **Payment Gateway**: Max 10 concurrent calls
- **Database Read**: Max 15 concurrent calls
- **Database Write**: Max 8 concurrent calls
- **Event Publishing**: Max 20 concurrent calls
- **MongoDB**: Max 12 concurrent calls
- **External Services**: Max 8 concurrent calls

#### Configuration Example
```yaml
resilience4j:
  bulkhead:
    instances:
      payment-gateway:
        max-concurrent-calls: 10
        max-wait-duration: 2s
```

### 3. Retry Pattern

Automatic retry with exponential backoff for transient failures:

#### Retry Strategies
- **Payment Gateway**: 3 attempts, 1s initial delay, 2x multiplier
- **Database Operations**: 2-3 attempts, 500ms-1s initial delay
- **Event Publishing**: 3 attempts, 800ms initial delay, 1.8x multiplier
- **MongoDB**: 3 attempts, 600ms initial delay, 1.6x multiplier

#### Retriable Exceptions
- `RuntimeException` (for payment gateway)
- `SQLException` and `DataAccessException` (for databases)
- `KafkaException` and `RetriableException` (for event publishing)
- `MongoException` (for MongoDB)
- Connection and timeout exceptions

### 4. Time Limiter Pattern

Prevents operations from running indefinitely:

#### Timeout Configuration
- **Payment Gateway**: 10 seconds
- **Database Operations**: 3-5 seconds
- **Event Publishing**: 2 seconds

## Service-Specific Implementations

### Payment Service

#### ResilientPaymentGateway
```java
@CircuitBreaker(name = "payment-gateway", fallbackMethod = "fallbackProcessPayment")
@Retry(name = "payment-gateway")
@Bulkhead(name = "payment-gateway", fallbackMethod = "fallbackProcessPayment")
@TimeLimiter(name = "payment-gateway")
public PaymentResult processPayment(PaymentRequest request) {
    return delegate.processPayment(request);
}
```

#### Fallback Behavior
- Returns `SERVICE_UNAVAILABLE` error
- Logs failure for monitoring
- Maintains audit trail

### Order Service

#### ResilientDatabaseService
Wraps database operations with resilience patterns:
```java
@CircuitBreaker(name = "database-read", fallbackMethod = "fallbackDatabaseRead")
@Retry(name = "database-read")
@Bulkhead(name = "database-read")
public <T> T executeRead(Supplier<T> operation, String operationName) {
    return operation.get();
}
```

#### ResilientEventPublisher
Wraps Kafka event publishing:
```java
@CircuitBreaker(name = "event-publisher", fallbackMethod = "fallbackPublish")
@Retry(name = "event-publisher")
@Bulkhead(name = "event-publisher")
public void publish(String topic, Object event) {
    delegate.publish(topic, event);
}
```

### Cart Service

#### ResilientCartService
Handles MongoDB and external service resilience:
```java
@CircuitBreaker(name = "mongodb", fallbackMethod = "fallbackMongoOperation")
@Retry(name = "mongodb")
@Bulkhead(name = "mongodb")
public <T> T executeMongoOperation(Supplier<T> operation, String operationName) {
    return operation.get();
}
```

## Fallback Strategies

### 1. Payment Gateway Fallback
- Return failure result with `SERVICE_UNAVAILABLE` code
- Include fallback indicator in response
- Maintain consistent error format

### 2. Database Fallback
- Throw `DatabaseUnavailableException`
- Log error for investigation
- Preserve original exception for debugging

### 3. Event Publishing Fallback
- Store failed events for later retry
- Throw `EventPublishingException`
- Send to dead letter queue (configurable)

### 4. External Service Fallback
- Return cached data (if available)
- Throw `ExternalServiceUnavailableException`
- Degrade functionality gracefully

## Monitoring and Observability

### Health Indicators
All resilience components expose health indicators:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,circuitbreakers,bulkheads,retries
  health:
    circuitbreakers:
      enabled: true
    bulkheads:
      enabled: true
```

### Metrics Endpoints
- `/actuator/circuitbreakers` - Circuit breaker states and metrics
- `/actuator/bulkheads` - Bulkhead usage statistics
- `/actuator/retries` - Retry attempt statistics
- `/actuator/health` - Overall health including resilience components

### Key Metrics
- Circuit breaker state transitions
- Failure rates and success rates
- Bulkhead utilization
- Retry attempt counts
- Slow call percentages

## Testing Strategy

### Unit Tests
- Test individual resilience patterns
- Verify fallback behavior
- Test configuration scenarios

### Integration Tests
- Test resilience patterns working together
- Verify circuit breaker state transitions
- Test bulkhead resource isolation
- Validate retry mechanisms

### Chaos Engineering
- Simulate service failures
- Test network partitions
- Validate timeout scenarios
- Test high load conditions

## Best Practices

### 1. Circuit Breaker Configuration
- Set appropriate failure thresholds based on SLA requirements
- Configure wait durations based on expected recovery time
- Use sliding windows appropriate for traffic patterns

### 2. Bulkhead Sizing
- Size bulkheads based on available resources
- Consider downstream service capacity
- Monitor utilization and adjust as needed

### 3. Retry Strategy
- Use exponential backoff to avoid thundering herd
- Limit retry attempts to prevent resource exhaustion
- Only retry on transient failures

### 4. Fallback Design
- Provide meaningful fallback responses
- Maintain data consistency in fallbacks
- Log fallback activations for monitoring

### 5. Monitoring
- Set up alerts for circuit breaker state changes
- Monitor bulkhead utilization trends
- Track retry success/failure rates
- Measure impact on user experience

## Configuration Management

### Environment-Specific Settings
Different environments may require different resilience settings:

#### Development
- Lower thresholds for faster feedback
- Shorter timeouts for quicker tests
- More verbose logging

#### Production
- Conservative thresholds based on SLA
- Longer timeouts for stability
- Optimized for performance

### Dynamic Configuration
Consider using configuration management tools for:
- Runtime adjustment of thresholds
- A/B testing of resilience settings
- Emergency circuit breaker activation

## Troubleshooting

### Common Issues

#### Circuit Breaker Stuck Open
- Check failure rate thresholds
- Verify downstream service health
- Review wait duration settings

#### Bulkhead Exhaustion
- Monitor concurrent call patterns
- Adjust max concurrent calls
- Check for resource leaks

#### Excessive Retries
- Review retry conditions
- Check exponential backoff settings
- Monitor downstream service capacity

### Debugging Tools
- Use actuator endpoints for real-time status
- Enable debug logging for Resilience4j
- Monitor application metrics and traces

## Future Enhancements

### Planned Improvements
1. **Rate Limiting** - Add rate limiting for API endpoints
2. **Cache Fallbacks** - Implement cache-based fallbacks
3. **Adaptive Thresholds** - Dynamic threshold adjustment
4. **Cross-Service Coordination** - Coordinated circuit breaker states

### Advanced Patterns
1. **Saga Circuit Breakers** - Circuit breakers for saga steps
2. **Distributed Circuit Breakers** - Shared circuit breaker state
3. **Predictive Failure Detection** - ML-based failure prediction
4. **Automated Recovery** - Self-healing mechanisms