# Order Saga Orchestrator

## Overview

The Order Saga Orchestrator implements the Saga pattern to manage distributed transactions across multiple microservices in the restaurant food ordering system. It ensures data consistency and handles failure scenarios through compensating actions.

## Saga Flow

### Happy Path
1. **Order Created** → Start saga orchestration
2. **Cart Validation** → Validate cart items and availability
3. **Payment Processing** → Process payment for the order
4. **Order Confirmation** → Confirm the order and complete saga

### Failure Scenarios
- **Cart Validation Failure** → Cancel order
- **Payment Failure** → Release cart items and cancel order
- **Order Confirmation Failure** → Refund payment and cancel order

## Components

### OrderSagaOrchestrator
- Main orchestrator class that manages the saga flow
- Handles Kafka events for each saga step
- Implements compensating actions for failures
- Maintains saga state throughout the process

### OrderSagaData
- Value object containing saga state information
- Tracks order details, current state, and failure reasons
- Includes retry count for error handling

### OrderSagaRepository
- Persistence layer for saga state
- MySQL-based implementation for ACID compliance
- Supports querying by state and timeout detection

### OrderSagaState
- Enum defining all possible saga states
- Includes both forward and compensating states
- Used for state machine transitions

## Kafka Topics

### Consumed Topics
- `cart-validation-completed` - Cart service responses
- `payment-processing-completed` - Payment service responses

### Published Topics
- `order-saga-started` - Saga initiation
- `cart-validation-requested` - Cart validation requests
- `payment-initiation-requested` - Payment processing requests
- `order-confirmed` - Successful order confirmation
- `order-cancelled` - Order cancellation (compensation)

## Error Handling

### Retry Mechanism
- Automatic retries for transient failures
- Exponential backoff strategy
- Maximum retry limits per saga step

### Timeout Handling
- Configurable timeouts for each saga step
- Automatic compensation for timed-out operations
- Dead letter queue for failed events

### Compensating Actions
- **Cart Validation Failure**: Publish OrderCancelledEvent
- **Payment Failure**: Release cart items and cancel order
- **General Failure**: Mark saga as failed and cancel order

## Configuration

The saga orchestrator is configured through:
- `SagaConfiguration` - Bean configuration
- `application.yml` - Kafka and database settings
- Database schema in `V1__Create_order_tables.sql`

## Testing

### Unit Tests
- `OrderSagaOrchestratorTest` - Tests saga logic with mocks
- Covers happy path and failure scenarios
- Verifies event publishing and state transitions

### Integration Tests
- `OrderSagaIntegrationTest` - Tests with real database
- Verifies saga repository operations
- Tests state persistence and retrieval

## Usage

The saga is automatically started when a new order is created through the `OrderCommandHandler`. The orchestrator then manages the entire flow asynchronously through Kafka events.

```java
// Saga is started automatically in OrderCommandHandler
sagaOrchestrator.startOrderSaga(
    order.getOrderId(),
    command.getCustomerId(),
    command.getRestaurantId(),
    command.getItems(),
    command.getTotalAmount()
);
```

## Monitoring

The saga orchestrator provides:
- Detailed logging at each step
- State tracking in the database
- Metrics for saga completion rates
- Alerts for failed sagas and timeouts