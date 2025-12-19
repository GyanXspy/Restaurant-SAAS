# Order Service

The Order Service is the saga orchestrator in the Restaurant Food Ordering System. It implements CQRS, Event Sourcing, and Saga patterns to manage the complete order processing workflow.

## Architecture

### CQRS (Command Query Responsibility Segregation)
- **Command Side**: Handles write operations through command handlers
- **Query Side**: Serves read operations from optimized read models
- **Event Store**: Persists all domain events for event sourcing

### Event Sourcing
- All state changes are captured as immutable events
- Aggregates are reconstructed by replaying events
- Complete audit trail of all order operations

### Saga Orchestration
- Manages distributed transactions across multiple services
- Implements compensating actions for failure scenarios
- Handles timeouts and retry mechanisms

## Package Structure

```
com.restaurant.order/
├── config/                 # Configuration classes
├── command/                # Command objects and handlers
├── query/                  # Query objects and handlers
├── domain/                 # Domain models and business logic
├── saga/                   # Saga orchestration logic
├── event/                  # Domain events
├── infrastructure/         # Infrastructure layer
│   ├── entity/            # JPA entities
│   └── repository/        # Data repositories
└── OrderServiceApplication.java
```

## Database Tables

### order_events
Event store table for event sourcing:
- Stores all domain events as JSON
- Supports event replay and aggregate reconstruction

### order_read_model
Optimized read model for queries:
- Denormalized data for fast queries
- Updated asynchronously from domain events

### order_saga_state
Saga state management:
- Tracks saga orchestration progress
- Handles timeouts and retries

## Key Features

1. **Event Sourcing**: Complete audit trail and state reconstruction
2. **CQRS**: Optimized read and write operations
3. **Saga Orchestration**: Distributed transaction management
4. **Resilience**: Timeout handling, retries, and compensating actions
5. **Scalability**: Stateless design with horizontal scaling support

## Configuration

The service requires:
- MySQL database for event store and read models
- Kafka for event publishing and consumption
- Configuration for saga timeouts and retry policies