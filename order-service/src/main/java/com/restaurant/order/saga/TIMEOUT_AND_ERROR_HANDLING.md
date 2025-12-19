# Saga Timeout and Error Handling Implementation

This document describes the timeout and error handling mechanisms implemented for the Order Service saga orchestration.

## Overview

The saga timeout and error handling system provides:
- **Timeout Management**: Configurable timeouts for each saga step
- **Retry Mechanisms**: Exponential backoff retry logic with configurable limits
- **Dead Letter Queue Handling**: Processing of failed events that couldn't be handled
- **Comprehensive Testing**: Unit, integration, and performance tests

## Components

### 1. SagaTimeoutConfig
Configuration class for timeout and retry settings.

**Key Properties:**
- `cart-validation`: Timeout for cart validation step (default: 2 minutes)
- `payment-processing`: Timeout for payment processing step (default: 5 minutes)
- `order-confirmation`: Timeout for order confirmation step (default: 1 minute)
- `max-retry-attempts`: Maximum number of retry attempts (default: 3)
- `initial-retry-delay`: Initial delay for first retry (default: 1 second)
- `retry-multiplier`: Multiplier for exponential backoff (default: 2.0)
- `max-retry-delay`: Maximum delay between retries (default: 5 minutes)

### 2. SagaTimeoutManager
Manages timeout scheduling and cancellation for saga steps.

**Key Features:**
- Schedules timeouts for each saga step
- Cancels timeouts when responses are received
- Handles timeout events and triggers appropriate actions
- Thread-safe timeout management using ConcurrentHashMap

### 3. SagaRetryManager
Handles retry logic with exponential backoff.

**Key Features:**
- Tracks retry counts per saga
- Implements exponential backoff algorithm
- Enforces retry limits
- Resets retry counts on successful operations

### 4. SagaDeadLetterQueueHandler
Processes events that failed to be handled normally.

**Key Features:**
- Handles DLQ events from cart validation, payment processing, and general saga events
- Extracts order IDs from failed event payloads
- Marks sagas as failed when DLQ events are received
- Provides statistics on failed events
- Supports manual reprocessing of failed events

### 5. Enhanced OrderSagaOrchestrator
Updated orchestrator with timeout and retry integration.

**Key Enhancements:**
- Schedules timeouts for each saga step
- Cancels timeouts when responses are received
- Implements retry logic for failed operations
- Uses @RetryableTopic annotation for Kafka retry handling
- Handles timeout events with appropriate compensating actions

## Configuration

### Application Properties (application.yml)
```yaml
saga:
  timeout:
    cart-validation: PT2M  # 2 minutes
    payment-processing: PT5M  # 5 minutes
    order-confirmation: PT1M  # 1 minute
    max-retry-attempts: 3
    initial-retry-delay: PT1S  # 1 second
    retry-multiplier: 2.0
    max-retry-delay: PT5M  # 5 minutes
```

### Kafka Topics for DLQ
- `cart-validation-completed-dlq`: Failed cart validation events
- `payment-processing-completed-dlq`: Failed payment processing events
- `saga-events-dlq`: Failed general saga events

## Timeout Handling Flow

1. **Step Initiation**: When a saga step starts, a timeout is scheduled
2. **Response Received**: If a response is received within the timeout, the timeout is cancelled
3. **Timeout Occurs**: If no response is received, the timeout handler is triggered
4. **Retry Logic**: The timeout handler checks if retries are available and schedules a retry
5. **Retry Limit Exceeded**: If retry limit is exceeded, compensating actions are triggered

## Retry Mechanism

### Exponential Backoff Algorithm
```
delay = initial_delay * (multiplier ^ (attempt - 1))
actual_delay = min(delay, max_delay)
```

### Example Retry Delays (with default config)
- Attempt 1: 1 second
- Attempt 2: 2 seconds  
- Attempt 3: 4 seconds
- Attempt 4+: Capped at 5 minutes

## Error Handling Scenarios

### 1. Cart Validation Timeout
- **Action**: Retry cart validation request
- **Retry Limit**: 3 attempts
- **Compensation**: Cancel order if retries exhausted

### 2. Payment Processing Timeout
- **Action**: Retry payment processing request
- **Retry Limit**: 3 attempts
- **Compensation**: Cancel order and release cart items if retries exhausted

### 3. Order Confirmation Timeout
- **Action**: Retry order confirmation
- **Retry Limit**: 3 attempts
- **Compensation**: Refund payment and cancel order if retries exhausted

### 4. Event Processing Failures
- **Action**: Events sent to Dead Letter Queue
- **Monitoring**: DLQ handler logs and tracks failed events
- **Recovery**: Manual reprocessing capability

## Monitoring and Observability

### Metrics Collected
- Saga completion rates
- Timeout occurrences per step
- Retry attempt counts
- DLQ event counts
- Processing latencies

### Logging
- Timeout events logged at WARN level
- Retry attempts logged at INFO level
- DLQ events logged at ERROR level
- Saga state changes logged at DEBUG level

### Health Checks
- Saga processing health indicator
- DLQ event count monitoring
- Timeout scheduling health

## Testing Strategy

### Unit Tests
- **SagaTimeoutConfigTest**: Configuration and retry delay calculation
- **SagaTimeoutAndRetryTest**: Timeout scheduling and retry logic
- **SagaDeadLetterQueueHandlerTest**: DLQ event handling

### Integration Tests
- **SagaIntegrationTest**: End-to-end saga flows with timeouts
- Complete success and failure scenarios
- Concurrent saga execution

### Performance Tests
- **SagaPerformanceTest**: High-volume order processing
- Retry performance under load
- Memory usage validation
- Timeout scheduling performance

## Best Practices

### Configuration
- Set timeouts based on expected service response times
- Configure retry limits to balance reliability and performance
- Use shorter timeouts in test environments

### Monitoring
- Set up alerts for high DLQ event rates
- Monitor saga completion rates
- Track timeout occurrence patterns

### Error Recovery
- Implement manual DLQ event reprocessing
- Provide saga state inspection tools
- Enable saga replay capabilities for critical failures

## Troubleshooting

### Common Issues

1. **High Timeout Rates**
   - Check downstream service health
   - Verify network connectivity
   - Review timeout configuration

2. **Retry Storms**
   - Verify exponential backoff configuration
   - Check retry limit settings
   - Monitor system resource usage

3. **DLQ Event Accumulation**
   - Investigate root cause of event processing failures
   - Check event serialization/deserialization
   - Verify Kafka consumer configuration

### Debugging Tools
- Saga state inspection endpoints
- DLQ event statistics API
- Timeout and retry metrics dashboard
- Distributed tracing for saga flows

## Future Enhancements

1. **Adaptive Timeouts**: Dynamic timeout adjustment based on historical performance
2. **Circuit Breaker Integration**: Prevent cascade failures during service outages
3. **Saga Replay**: Ability to replay failed sagas from specific points
4. **Advanced Monitoring**: Real-time saga flow visualization
5. **Auto-Recovery**: Automatic DLQ event reprocessing with intelligent filtering