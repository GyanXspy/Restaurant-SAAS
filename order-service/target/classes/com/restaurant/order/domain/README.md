# Order Aggregate - Event Sourcing Implementation

## Overview

The Order aggregate is the core domain entity in the Order Service, implementing the Event Sourcing pattern to ensure complete auditability and state reconstruction capabilities. This implementation follows Domain-Driven Design (DDD) principles and provides strong consistency guarantees through event-driven state management.

## Key Components

### 1. Order Aggregate Root (`Order.java`)

The Order aggregate is the main entity that encapsulates all business logic related to order processing. Key features:

- **Event Sourcing**: All state changes are captured as immutable domain events
- **Aggregate Reconstruction**: Can rebuild current state by replaying events
- **Business Logic Encapsulation**: All order-related business rules are contained within the aggregate
- **Immutable State Changes**: State can only be changed through domain events

#### Core Methods:

- `createOrder()`: Factory method to create new orders
- `confirmOrder()`: Confirms order after successful payment
- `cancelOrder()`: Cancels order with reason
- `fromEvents()`: Reconstructs aggregate from event history
- `getUncommittedEvents()`: Returns events that need to be persisted

### 2. Order Business Rules (`OrderBusinessRules.java`)

Centralized validation and business logic to ensure consistency:

- **Order Creation Validation**: Validates customer ID, restaurant ID, items, and total amount
- **Order Confirmation Validation**: Ensures order can be confirmed and payment ID is valid
- **Order Cancellation Validation**: Prevents cancellation of confirmed orders
- **Total Amount Validation**: Ensures provided total matches calculated total

### 3. Order Event Store (`OrderEventStore.java`)

Repository implementation for persisting and retrieving Order aggregates using event sourcing:

- **Event Persistence**: Saves uncommitted events to the event store
- **Aggregate Loading**: Reconstructs aggregates by replaying events
- **Version Management**: Supports loading from specific versions
- **Error Handling**: Provides comprehensive error handling with custom exceptions

### 4. Domain Events

Three main events capture all order state changes:

#### OrderCreatedEvent
- Triggered when a new order is created
- Contains: customer ID, restaurant ID, items, total amount
- Initial version: 1

#### OrderConfirmedEvent
- Triggered when order is successfully confirmed
- Contains: payment ID, confirmation details
- Increments version

#### OrderCancelledEvent
- Triggered when order is cancelled
- Contains: cancellation reason
- Increments version

## Event Sourcing Workflow

### 1. Order Creation
```java
// Create new order
Order order = Order.createOrder(customerId, restaurantId, items, totalAmount);

// Get uncommitted events
List<DomainEvent> events = order.getUncommittedEvents();

// Persist events
orderEventStore.save(order);
```

### 2. Order State Changes
```java
// Load existing order
Optional<Order> orderOpt = orderEventStore.findById(orderId);
Order order = orderOpt.get();

// Make state change
order.confirmOrder(paymentId);

// Persist new events
orderEventStore.save(order);
```

### 3. Aggregate Reconstruction
```java
// Load all events for order
List<DomainEvent> events = orderEventStore.getOrderEvents(orderId);

// Reconstruct aggregate
Order order = Order.fromEvents(events);
```

## Business Rules

### Order Creation Rules
1. Customer ID is required and cannot be empty
2. Restaurant ID is required and cannot be empty
3. Order must contain at least one item
4. Total amount must be greater than zero
5. Provided total must match calculated total from items

### Order Confirmation Rules
1. Order must be in PENDING status
2. Payment ID is required and cannot be empty
3. Only one confirmation per order is allowed

### Order Cancellation Rules
1. Cannot cancel confirmed orders
2. Cancelling already cancelled orders is a no-op
3. Cancellation reason is recorded in the event

## State Transitions

```
PENDING → CONFIRMED (via confirmOrder)
PENDING → CANCELLED (via cancelOrder)
CONFIRMED → [FINAL STATE]
CANCELLED → [FINAL STATE]
```

## Error Handling

### Domain Validation Errors
- `IllegalArgumentException`: Invalid input parameters
- `IllegalStateException`: Invalid state transitions

### Persistence Errors
- `OrderPersistenceException`: Event store operation failures
- Wraps underlying database exceptions with context

## Testing Strategy

### Unit Tests (`OrderTest.java`)
- Tests all business logic and validation rules
- Verifies event sourcing behavior
- Tests error conditions and edge cases

### Integration Tests (`OrderIntegrationTest.java`)
- Tests complete order lifecycle
- Verifies event sourcing reconstruction
- Tests cancellation scenarios

### Event Store Tests (`OrderEventStoreTest.java`)
- Tests persistence and retrieval operations
- Verifies error handling
- Tests version management

## Usage Examples

### Creating an Order
```java
List<OrderItem> items = Arrays.asList(
    new OrderItem("item-1", "Pizza", new BigDecimal("15.99"), 1),
    new OrderItem("item-2", "Drink", new BigDecimal("3.99"), 2)
);

BigDecimal total = new BigDecimal("23.97");
Order order = Order.createOrder("customer-123", "restaurant-456", items, total);
```

### Processing Order Through Saga
```java
// 1. Create order
Order order = Order.createOrder(customerId, restaurantId, items, total);
orderEventStore.save(order);

// 2. After successful payment processing
order = orderEventStore.findById(orderId).get();
order.confirmOrder(paymentId);
orderEventStore.save(order);
```

### Handling Failures
```java
// Cancel order due to payment failure
order = orderEventStore.findById(orderId).get();
order.cancelOrder("Payment processing failed");
orderEventStore.save(order);
```

## Performance Considerations

1. **Event Loading**: Events are loaded in chronological order for reconstruction
2. **Snapshot Strategy**: Consider implementing snapshots for orders with many events
3. **Caching**: Aggregate instances can be cached to avoid repeated event replay
4. **Batch Processing**: Multiple events can be saved atomically

## Future Enhancements

1. **Snapshot Support**: Implement periodic snapshots to improve performance
2. **Event Versioning**: Support for event schema evolution
3. **Soft Deletion**: Implement soft deletion for cancelled orders
4. **Audit Trail**: Enhanced audit capabilities with user tracking