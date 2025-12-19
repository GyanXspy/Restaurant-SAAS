# Shared Events Library

This module provides the core event infrastructure for the Restaurant Food Ordering System, implementing event sourcing, CQRS, and saga orchestration patterns.

## Features

- **Domain Events**: Base classes and interfaces for domain events
- **Event Store**: MySQL-based event store with event sourcing support
- **Event Publishing**: Kafka-based event publishing with retry logic and dead letter queue
- **Event Versioning**: Schema evolution and backward compatibility support
- **Idempotent Processing**: Ensures events are processed exactly once
- **Dead Letter Queue**: Handles failed events for manual intervention

## Components

### Domain Events

All domain events extend the `DomainEvent` base class:

```java
@EventVersion(1)
public class OrderCreatedEvent extends DomainEvent {
    // Event properties
}
```

### Event Store

The `EventStore` interface provides methods for storing and retrieving events:

```java
@Autowired
private EventStore eventStore;

// Save events
eventStore.saveEvent(event);
eventStore.saveEvents(events);

// Retrieve events
List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId);
```

### Event Publishing

The `EventPublisher` interface supports both synchronous and asynchronous publishing:

```java
@Autowired
private EventPublisher eventPublisher;

// Synchronous publishing
eventPublisher.publish(event);

// Asynchronous publishing
CompletableFuture<Void> future = eventPublisher.publishAsync(event);
```

### Idempotent Processing

Use `IdempotentEventProcessor` to ensure events are processed exactly once:

```java
@Autowired
private IdempotentEventProcessor processor;

boolean processed = processor.processEvent(event);
```

## Configuration

Add the following properties to your `application.yml`:

```yaml
restaurant:
  events:
    enabled: true
    store:
      type: mysql
    publisher:
      type: kafka
    idempotency:
      enabled: true

spring:
  kafka:
    producer:
      bootstrap-servers: localhost:9092
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
```

## Database Setup

Run the SQL script to create the required tables:

```sql
-- From shared-events/src/main/resources/sql/create-event-store.sql
CREATE TABLE events (...);
CREATE TABLE processed_events (...);
CREATE TABLE failed_events (...);
```

## Usage in Services

1. Add dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>com.restaurant</groupId>
    <artifactId>shared-events</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

2. Enable event infrastructure in your configuration:

```java
@EnableAutoConfiguration
@ComponentScan(basePackages = {"com.restaurant.events"})
public class ServiceApplication {
    // Application configuration
}
```

3. Inject and use event components:

```java
@Service
public class OrderService {
    
    @Autowired
    private EventStore eventStore;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    public void createOrder(CreateOrderCommand command) {
        // Business logic
        OrderCreatedEvent event = new OrderCreatedEvent(...);
        
        // Store event
        eventStore.saveEvent(event);
        
        // Publish event
        eventPublisher.publish(event);
    }
}
```

## Event Topics

The system uses the following Kafka topics:

- `order-saga-started`: Order saga initiation
- `cart-validation-requested`: Cart validation requests
- `cart-validation-completed`: Cart validation responses
- `payment-initiation-requested`: Payment processing requests
- `payment-processing-completed`: Payment processing responses
- `order-confirmed`: Order confirmations
- `order-cancelled`: Order cancellations
- `user-events`: User service events
- `restaurant-events`: Restaurant service events
- `restaurant-events-dlq`: Dead letter queue for failed events

## Error Handling

The system provides comprehensive error handling:

- **Retry Logic**: Automatic retries with exponential backoff
- **Dead Letter Queue**: Failed events are sent to DLQ for manual processing
- **Idempotency**: Duplicate events are automatically detected and ignored
- **Circuit Breaker**: Prevents cascade failures (when configured)

## Testing

The library includes test utilities for event testing:

```java
@Test
public void testEventProcessing() {
    DomainEvent event = new OrderCreatedEvent(...);
    
    // Test event serialization
    EventSerializer serializer = new EventSerializer();
    String json = serializer.serialize(event);
    DomainEvent deserialized = serializer.deserialize(json);
    
    // Test event store
    eventStore.saveEvent(event);
    List<DomainEvent> events = eventStore.getEventsForAggregate(aggregateId);
}
```