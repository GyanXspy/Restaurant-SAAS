# Design Document

## Overview

The Restaurant Food Ordering System implements a high-scale, event-driven microservices architecture using Spring Boot, Apache Kafka, and advanced patterns including Saga Orchestration, CQRS, and Event Sourcing. The system is designed to handle massive traffic loads while ensuring data consistency across five independent microservices.

## Architecture

### High-Level Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │  Load Balancer  │    │   Monitoring    │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
    ┌────────────────────────────┼────────────────────────────┐
    │                            │                            │
    │              Apache Kafka Event Bus                     │
    │                                                         │
    └─────────────────────────────────────────────────────────┘
         │         │         │         │         │
    ┌────▼───┐ ┌───▼───┐ ┌───▼───┐ ┌───▼────┐ ┌──▼─────┐
    │ User   │ │Restaurant│ Cart  │ │ Order  │ │Payment │
    │Service │ │ Service │Service│ │Service │ │Service │
    │        │ │         │       │ │(Saga   │ │        │
    │MongoDB │ │ MongoDB │MongoDB│ │Orchestr)│ │ MySQL  │
    └────────┘ └─────────┘ └───────┘ │ MySQL  │ └────────┘
                                     └────────┘
```

### Microservices Architecture

#### 1. User Service
- **Responsibility**: User registration, authentication, profile management
- **Database**: MongoDB (flexible schema for user profiles)
- **Patterns**: CQRS for user queries and commands

#### 2. Restaurant Service
- **Responsibility**: Restaurant management, menu items, availability
- **Database**: MongoDB (flexible schema for restaurant data and menus)
- **Patterns**: Event publishing for menu updates

#### 3. Cart Service
- **Responsibility**: Shopping cart management, item validation
- **Database**: MongoDB (session-based cart storage)
- **Patterns**: Event-driven cart validation

#### 4. Food Order Service (Saga Orchestrator)
- **Responsibility**: Order processing, saga orchestration, order state management
- **Database**: MySQL (ACID compliance for order consistency)
- **Patterns**: Saga Orchestration, CQRS, Event Sourcing

#### 5. Payment Service
- **Responsibility**: Payment processing, transaction management
- **Database**: MySQL (ACID compliance for financial transactions)
- **Patterns**: Event Sourcing for payment audit trail

## Components and Interfaces

### Saga Orchestration Pattern

The Food Order Service acts as the Saga Orchestrator managing the following steps:

#### Saga Steps and Kafka Topics

1. **Order Created**
   - Topic: `order-saga-started`
   - Event: `OrderSagaStartedEvent`
   - Action: Initialize order saga

2. **Cart Validation**
   - Topic: `cart-validation-requested`
   - Event: `CartValidationRequestedEvent`
   - Response Topic: `cart-validation-completed`
   - Compensating Action: Cancel order saga

3. **Payment Initiation**
   - Topic: `payment-initiation-requested`
   - Event: `PaymentInitiationRequestedEvent`
   - Response Topic: `payment-processing-completed`
   - Compensating Action: Release cart items

4. **Order Confirmation**
   - Topic: `order-confirmation-requested`
   - Event: `OrderConfirmationRequestedEvent`
   - Success Topic: `order-confirmed`
   - Failure Topic: `order-cancelled`

#### Compensating Actions

- **Cart Validation Failure**: Publish `OrderCancelledEvent`
- **Payment Failure**: Publish `CartItemsReleasedEvent`, `OrderCancelledEvent`
- **Order Confirmation Failure**: Publish `PaymentRefundRequestedEvent`, `OrderCancelledEvent`

### CQRS Implementation

#### Command Side (Write Operations)
```java
// Command handlers process business logic
@Component
public class OrderCommandHandler {
    public void handle(CreateOrderCommand command) {
        // Business logic
        // Event persistence
        // Event publishing
    }
}
```

#### Query Side (Read Operations)
```java
// Query handlers serve optimized read models
@Component
public class OrderQueryHandler {
    public OrderView getOrder(String orderId) {
        // Serve from read model
    }
}
```

#### Read Model Projections
- **OrderReadModel**: Optimized for order queries
- **UserOrderHistoryModel**: Optimized for user order history
- **RestaurantOrdersModel**: Optimized for restaurant order management

### Event Sourcing Implementation

#### Event Store Structure (MySQL)
```sql
CREATE TABLE order_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSON NOT NULL,
    event_version INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_aggregate_id (aggregate_id),
    INDEX idx_event_type (event_type)
);
```

#### Event Classes
```java
public abstract class DomainEvent {
    private String eventId;
    private String aggregateId;
    private LocalDateTime occurredOn;
    private int version;
}

public class OrderCreatedEvent extends DomainEvent {
    private String customerId;
    private String restaurantId;
    private List<OrderItem> items;
    private BigDecimal totalAmount;
}

public class PaymentCompletedEvent extends DomainEvent {
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private PaymentStatus status;
}
```

## Data Models

### Database Design Rationale

#### MySQL for Order and Payment Services
- **ACID Compliance**: Critical for financial transactions and order consistency
- **Strong Consistency**: Required for saga orchestration state management
- **Relational Integrity**: Order-payment relationships need referential integrity

#### MongoDB for User, Restaurant, and Cart Services
- **Schema Flexibility**: User profiles and restaurant menus have varying structures
- **Horizontal Scaling**: Better performance for read-heavy operations
- **Document Storage**: Natural fit for nested cart items and menu structures

### Order Service Schema (MySQL)
```sql
-- Command side tables
CREATE TABLE orders (
    id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    restaurant_id VARCHAR(255) NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED') NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Query side read model
CREATE TABLE order_read_model (
    id VARCHAR(255) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    restaurant_name VARCHAR(255) NOT NULL,
    items JSON NOT NULL,
    status VARCHAR(50) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP,
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status)
);
```

### Payment Service Schema (MySQL)
```sql
CREATE TABLE payments (
    id VARCHAR(255) PRIMARY KEY,
    order_id VARCHAR(255) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED') NOT NULL,
    payment_method VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSON NOT NULL,
    event_version INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### MongoDB Collections

#### User Service
```javascript
// users collection
{
  "_id": ObjectId,
  "userId": "string",
  "email": "string",
  "profile": {
    "firstName": "string",
    "lastName": "string",
    "phone": "string",
    "addresses": [
      {
        "type": "string",
        "street": "string",
        "city": "string",
        "zipCode": "string"
      }
    ]
  },
  "createdAt": Date,
  "updatedAt": Date
}
```

#### Restaurant Service
```javascript
// restaurants collection
{
  "_id": ObjectId,
  "restaurantId": "string",
  "name": "string",
  "cuisine": "string",
  "address": {
    "street": "string",
    "city": "string",
    "zipCode": "string"
  },
  "menu": [
    {
      "itemId": "string",
      "name": "string",
      "description": "string",
      "price": Number,
      "category": "string",
      "available": Boolean
    }
  ],
  "isActive": Boolean,
  "createdAt": Date,
  "updatedAt": Date
}
```

#### Cart Service
```javascript
// carts collection
{
  "_id": ObjectId,
  "cartId": "string",
  "customerId": "string",
  "restaurantId": "string",
  "items": [
    {
      "itemId": "string",
      "name": "string",
      "price": Number,
      "quantity": Number
    }
  ],
  "totalAmount": Number,
  "createdAt": Date,
  "updatedAt": Date,
  "expiresAt": Date
}
```

## Error Handling

### Saga Error Handling
- **Timeout Handling**: Each saga step has configurable timeouts
- **Retry Mechanism**: Exponential backoff for transient failures
- **Compensating Actions**: Automatic rollback on saga failure
- **Dead Letter Queue**: Failed events moved to DLQ for manual intervention

### Event Processing Error Handling
- **Idempotent Processing**: Event deduplication using event IDs
- **Poison Message Handling**: Malformed events sent to error topics
- **Circuit Breaker**: Prevent cascade failures between services
- **Graceful Degradation**: Fallback mechanisms for service unavailability

## Testing Strategy

### Unit Testing
- **Domain Logic**: Test aggregates and domain services in isolation
- **Event Handlers**: Test event processing logic with mock dependencies
- **Saga Orchestrator**: Test saga steps and compensating actions

### Integration Testing
- **Kafka Integration**: Test event publishing and consumption
- **Database Integration**: Test repository operations and transactions
- **API Integration**: Test REST endpoints and request/response handling

### End-to-End Testing
- **Saga Flow Testing**: Test complete order processing flow
- **Event Sourcing Testing**: Test event replay and aggregate reconstruction
- **Performance Testing**: Load testing for high-traffic scenarios

### Testing Infrastructure
- **Testcontainers**: Containerized databases and Kafka for integration tests
- **WireMock**: Mock external payment gateways
- **Test Data Builders**: Consistent test data creation patterns
## Ka
fka Event Flow

### Event Flow Diagram (Text Format)

```
Customer Order Flow:
1. Customer → API Gateway → Order Service: POST /orders
2. Order Service → Kafka: OrderSagaStartedEvent (order-saga-started)
3. Order Service → Kafka: CartValidationRequestedEvent (cart-validation-requested)
4. Cart Service ← Kafka: CartValidationRequestedEvent
5. Cart Service → Kafka: CartValidationCompletedEvent (cart-validation-completed)
6. Order Service ← Kafka: CartValidationCompletedEvent
7. Order Service → Kafka: PaymentInitiationRequestedEvent (payment-initiation-requested)
8. Payment Service ← Kafka: PaymentInitiationRequestedEvent
9. Payment Service → Kafka: PaymentProcessingCompletedEvent (payment-processing-completed)
10. Order Service ← Kafka: PaymentProcessingCompletedEvent
11. Order Service → Kafka: OrderConfirmedEvent (order-confirmed)
12. All Services ← Kafka: OrderConfirmedEvent (for read model updates)
```

### Kafka Topics and Events

#### Core Topics
- **order-saga-started**: Saga initiation events
- **cart-validation-requested**: Cart validation requests
- **cart-validation-completed**: Cart validation responses
- **payment-initiation-requested**: Payment processing requests
- **payment-processing-completed**: Payment processing responses
- **order-confirmed**: Successful order confirmations
- **order-cancelled**: Order cancellation events

#### Event Versioning Strategy
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "eventType")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderCreatedEventV1.class, name = "OrderCreatedV1"),
    @JsonSubTypes.Type(value = OrderCreatedEventV2.class, name = "OrderCreatedV2")
})
public abstract class OrderCreatedEvent extends DomainEvent {
    // Base event structure
}
```

#### Kafka Configuration
```yaml
# Producer Configuration
spring:
  kafka:
    producer:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 1

# Consumer Configuration
    consumer:
      bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS}
      group-id: ${spring.application.name}
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      auto-offset-reset: earliest
      enable-auto-commit: false
      properties:
        spring.json.trusted.packages: "com.restaurant.events"
```

## Scalability and High-Load Handling

### Horizontal Scaling Strategies

#### Service Scaling
- **Stateless Services**: All services designed to be stateless for easy horizontal scaling
- **Load Balancing**: Round-robin and least-connections algorithms
- **Auto-scaling**: Kubernetes HPA based on CPU/memory metrics
- **Circuit Breakers**: Prevent cascade failures during high load

#### Database Scaling
- **MySQL Read Replicas**: Separate read and write operations
- **MongoDB Sharding**: Horizontal partitioning for large collections
- **Connection Pooling**: Optimized database connection management
- **Query Optimization**: Proper indexing and query patterns

#### Kafka Scaling
- **Partition Strategy**: Partition by customer ID for even load distribution
- **Consumer Groups**: Multiple consumer instances per service
- **Batch Processing**: Process events in batches for better throughput
- **Compression**: Enable compression for network efficiency

### Performance Optimizations

#### Caching Strategy
- **Redis Cache**: Cache frequently accessed data (user profiles, menu items)
- **Application-Level Caching**: In-memory caching for read models
- **CDN**: Static content delivery for restaurant images and assets

#### Database Optimizations
- **Indexing Strategy**: Composite indexes for complex queries
- **Denormalization**: Strategic denormalization in read models
- **Archival Strategy**: Move old orders to archive tables

## Best Practices for Failure Handling and Retries

### Retry Mechanisms

#### Exponential Backoff
```java
@Retryable(
    value = {TransientException.class},
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
public void processEvent(DomainEvent event) {
    // Event processing logic
}
```

#### Dead Letter Queue Pattern
```java
@KafkaListener(topics = "order-processing-dlq")
public void handleFailedEvents(DomainEvent event) {
    // Log error details
    // Send alerts
    // Store for manual processing
}
```

### Circuit Breaker Implementation
```java
@CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackPayment")
public PaymentResult processPayment(PaymentRequest request) {
    // Payment processing logic
}

public PaymentResult fallbackPayment(PaymentRequest request, Exception ex) {
    // Fallback logic - queue for later processing
    return PaymentResult.queued();
}
```

### Monitoring and Alerting

#### Health Checks
- **Service Health**: Spring Boot Actuator endpoints
- **Database Health**: Connection pool monitoring
- **Kafka Health**: Consumer lag monitoring

#### Metrics Collection
- **Application Metrics**: Custom business metrics (orders/minute, payment success rate)
- **Infrastructure Metrics**: CPU, memory, disk usage
- **Event Processing Metrics**: Event processing latency, error rates

#### Alerting Rules
- **High Error Rates**: Alert when error rate exceeds 5%
- **Consumer Lag**: Alert when Kafka consumer lag exceeds threshold
- **Database Performance**: Alert on slow queries or connection pool exhaustion
- **Saga Timeouts**: Alert on saga processing timeouts

### Data Consistency Guarantees

#### Eventual Consistency
- **Read Model Updates**: Asynchronous projection updates
- **Cross-Service Data**: Eventually consistent through events
- **Conflict Resolution**: Last-writer-wins with timestamps

#### Strong Consistency
- **Within Service**: ACID transactions within service boundaries
- **Saga Orchestration**: Compensating actions ensure consistency
- **Event Ordering**: Kafka partition ordering for related events