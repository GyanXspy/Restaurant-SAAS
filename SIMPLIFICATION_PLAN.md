# Project Simplification Plan

## What We're Removing:
1. ❌ CQRS Pattern (command/query separation)
2. ❌ Event Sourcing (OrderEventStore)
3. ❌ Saga Pattern (distributed transactions)
4. ❌ Read/Write Database Separation
5. ❌ Complex Domain-Driven Design
6. ❌ Resilience4j Circuit Breakers
7. ❌ Complex projection handlers

## New Simple Structure:

```
order-service/
├── controller/
│   └── OrderController.java (Simple REST endpoints)
├── service/
│   └── OrderService.java (Business logic)
├── repository/
│   └── OrderRepository.java (JPA repository)
├── model/
│   ├── Order.java (Simple entity)
│   └── OrderItem.java (Simple entity)
├── dto/
│   ├── CreateOrderRequest.java
│   └── OrderResponse.java
└── config/
    └── KafkaConfig.java (Simple Kafka setup)
```

## Benefits:
✅ Easy to understand
✅ Easy to maintain
✅ Fast development
✅ Standard Spring Boot patterns
✅ Direct database operations
✅ Simple event publishing

## Implementation Steps:
1. Create new simplified structure
2. Move essential business logic
3. Delete complex pattern folders
4. Update configurations
5. Test basic CRUD operations
