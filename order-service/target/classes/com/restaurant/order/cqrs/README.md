# CQRS Implementation for Order Service

This package implements the Command Query Responsibility Segregation (CQRS) pattern for the Order Service.

## Architecture Overview

The CQRS pattern separates read and write operations into different models:

### Command Side (Write Operations)
- **OrderCommandHandler**: Handles all write operations (create, confirm, cancel orders)
- **Commands**: CreateOrderCommand, ConfirmOrderCommand, CancelOrderCommand
- **Domain Model**: Order aggregate with business logic
- **Event Store**: Persists domain events for event sourcing

### Query Side (Read Operations)
- **OrderQueryHandler**: Handles all read operations (search, retrieve orders)
- **Read Models**: OrderReadModel (JPA entity optimized for queries)
- **Projections**: OrderProjectionHandler updates read models from domain events
- **Views**: OrderView (DTO for API responses)

## Key Components

### Command Handler
```java
@Service
public class OrderCommandHandler {
    public String handle(CreateOrderCommand command) {
        // Create Order aggregate
        // Save to event store
        // Publish domain events
    }
}
```

### Query Handler
```java
@Service
public class OrderQueryHandler {
    public Optional<OrderView> handle(OrderQuery query) {
        // Query optimized read models
        // Return view objects
    }
}
```

### Event Projections
```java
@Service
public class OrderProjectionHandler {
    @KafkaListener(topics = "order-created")
    public void handle(OrderCreatedEvent event) {
        // Update read model from domain event
    }
}
```

## Benefits

1. **Scalability**: Read and write operations can be scaled independently
2. **Performance**: Read models are optimized for specific query patterns
3. **Flexibility**: Different storage technologies for read/write sides
4. **Eventual Consistency**: Read models are updated asynchronously

## Data Flow

1. Client sends command to OrderCommandController
2. Controller delegates to OrderCommandHandler
3. Handler creates/modifies Order aggregate
4. Domain events are persisted and published
5. OrderProjectionHandler receives events via Kafka
6. Read models are updated asynchronously
7. Queries are served from optimized read models

## Database Schema

### Write Side (Event Store)
- `order_events`: Stores all domain events
- Events are replayed to reconstruct aggregate state

### Read Side (Query Models)
- `order_read_model`: Denormalized view optimized for queries
- Includes indexes for common query patterns

## API Endpoints

### Command Endpoints (Write)
- `POST /api/orders` - Create order
- `PUT /api/orders/{id}/confirm` - Confirm order
- `PUT /api/orders/{id}/cancel` - Cancel order

### Query Endpoints (Read)
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders` - Search orders with filters
- `GET /api/orders/customers/{id}/recent` - Recent orders for customer
- `GET /api/orders/restaurants/{id}/active` - Active orders for restaurant