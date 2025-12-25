# Kafka Event Sourcing Integration - Order Service

## Overview
The Order Service implements **Event Sourcing with Kafka** for reliable event publishing and distributed system communication.

## Architecture Flow

```
Command → Event Store (MySQL) → Kafka Topic → Other Services
                ↓
         Current State (MySQL)
                ↓
         Read Model (MongoDB)
```

## Event Publishing Flow

1. **Command Received** - REST API receives command (CreateOrder, ConfirmOrder, CancelOrder)
2. **Event Created** - Domain event is created (OrderCreatedEvent, OrderConfirmedEvent, OrderCancelledEvent)
3. **Event Stored** - Event saved to MySQL event_store table (immutable audit trail)
4. **State Updated** - Current state snapshot saved to MySQL orders table
5. **Read Model Projected** - MongoDB read model updated for queries
6. **Kafka Published** - Event published to Kafka topic for other services

## Kafka Configuration

### Topics
- **order-events** - Main topic for all order domain events
- **order-events-dlq** - Dead letter queue for failed events

### Properties (application.properties)
```properties
# Kafka Bootstrap
spring.kafka.bootstrap-servers=localhost:9092

# Producer Configuration
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer

# Topics
kafka.topic.order-events=order-events
kafka.topic.dead-letter=order-events-dlq
```

## Event Structure

All events extend `DomainEvent` from shared-events library:

```json
{
  "eventId": "uuid",
  "aggregateId": "order-123",
  "occurredOn": "2025-12-25T10:00:00",
  "version": 1,
  "eventType": "OrderCreated",
  "customerId": "customer-456",
  "restaurantId": "restaurant-789",
  "totalAmount": 45.99,
  "items": [...]
}
```

## Key Components

### 1. KafkaConfig
- Configures EventPublisher bean
- Sets up EventSerializer for JSON serialization
- Defines TopicResolver for routing events

### 2. EventPublisher (from shared-events)
- `publish(DomainEvent)` - Synchronous publishing with retry
- `publishAsync(DomainEvent)` - Asynchronous publishing
- Automatic dead letter queue on failure
- Retry logic with exponential backoff

### 3. OrderCommandHandler
- Creates domain events
- Saves to event store
- Publishes to Kafka
- Updates projections

## Event Types

### OrderCreatedEvent
```java
new OrderCreatedEvent(orderId, customerId, restaurantId, totalAmount, items, version)
```
Published when: New order is created

### OrderConfirmedEvent
```java
new OrderConfirmedEvent(orderId, paymentId, version)
```
Published when: Order is confirmed with payment

### OrderCancelledEvent
```java
new OrderCancelledEvent(orderId, reason, version)
```
Published when: Order is cancelled

## Reliability Features

### 1. Retry Mechanism
- 3 retry attempts with exponential backoff
- Delays: 1s, 2s, 4s

### 2. Dead Letter Queue
- Failed events sent to DLQ topic
- Includes failure reason and timestamp
- Manual processing/replay capability

### 3. Partition Key
- Uses aggregateId (orderId) as partition key
- Ensures event ordering per order
- Enables parallel processing across orders

## Testing Kafka Integration

### 1. Start Kafka
```bash
# Using Docker
docker-compose up -d kafka zookeeper
```

### 2. Create Order (Publishes OrderCreatedEvent)
```bash
POST http://localhost:8084/api/orders
Content-Type: application/json

{
  "customerId": "customer-123",
  "restaurantId": "restaurant-456",
  "totalAmount": 45.99,
  "items": [
    {
      "menuItemId": "item-1",
      "name": "Pizza",
      "price": 12.99,
      "quantity": 2
    }
  ]
}
```

### 3. Consume Events
```bash
# Console consumer
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-events \
  --from-beginning \
  --property print.key=true
```

## Event Consumers (Other Services)

Other services can consume order events:

```java
@KafkaListener(topics = "order-events", groupId = "payment-service")
public void handleOrderEvent(String eventJson) {
    // Deserialize and process event
    OrderEvent event = eventSerializer.deserialize(eventJson);
    
    if (event instanceof OrderCreatedEvent) {
        // Process order created
    }
}
```

## Benefits

1. **Decoupling** - Services communicate via events, not direct calls
2. **Scalability** - Kafka handles high throughput
3. **Reliability** - Events are persisted and can be replayed
4. **Audit Trail** - Complete history in event store + Kafka
5. **Eventual Consistency** - Services update asynchronously
6. **Fault Tolerance** - DLQ and retry mechanisms

## Monitoring

### Kafka Metrics
- Event publish rate
- Consumer lag
- DLQ message count
- Retry attempts

### Application Logs
```
Published OrderCreatedEvent to Kafka for order: order-123
Successfully published event abc-123 to topic order-events at offset 42
```

## Next Steps

- [ ] Add event consumers in other services
- [ ] Implement event replay functionality
- [ ] Add Kafka monitoring dashboard
- [ ] Implement event schema versioning
- [ ] Add event encryption for sensitive data

---

**Event Sourcing + Kafka = Reliable, Scalable, Auditable Microservices!**
