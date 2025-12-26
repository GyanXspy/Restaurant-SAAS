# Kafka Topics - Restaurant SAAS Platform

## Overview
This document lists all Kafka topics used across the microservices architecture.

---

## Order Service Topics

### Saga Orchestration Topics
| Topic Name | Event Type | Producer | Consumer | Purpose |
|------------|------------|----------|----------|---------|
| `order-saga-started` | OrderCreated | order-service | order-service (saga orchestrator) | Initiates order processing saga |
| `cart-validation-requested` | CartValidationRequested | order-service | cart-service | Requests cart validation |
| `cart-validation-completed` | CartValidationCompleted | cart-service | order-service | Returns cart validation result |
| `payment-initiation-requested` | PaymentInitiationRequested | order-service | payment-service | Requests payment processing |
| `payment-processing-completed` | PaymentProcessingCompleted | payment-service | order-service | Returns payment processing result |
| `order-confirmed` | OrderConfirmed | order-service | notification-service, restaurant-service | Order successfully confirmed |
| `order-cancelled` | OrderCancelled | order-service | notification-service, cart-service | Order cancelled/failed |

### Order Event Topics
| Topic Name | Event Type | Producer | Consumer | Purpose |
|------------|------------|----------|----------|---------|
| `restaurant-events-ordercreated` | OrderCreated | order-service | analytics, notification | New order created |
| `restaurant-events-orderconfirmed` | OrderConfirmed | order-service | analytics, notification | Order confirmed |
| `restaurant-events-ordercancelled` | OrderCancelled | order-service | analytics, notification | Order cancelled |

### Dead Letter Queue
| Topic Name | Purpose |
|------------|---------|
| `order-service-dlq` | Failed order events for manual processing |

---

## Cart Service Topics

### Cart Event Topics
| Topic Name | Event Type | Producer | Consumer | Purpose |
|------------|------------|----------|----------|---------|
| `restaurant-events-cartitemadded` | CartItemAdded | cart-service | analytics | Item added to cart |
| `restaurant-events-cartitemremoved` | CartItemRemoved | cart-service | analytics | Item removed from cart |
| `restaurant-events-cartcleared` | CartCleared | cart-service | analytics | Cart cleared |

### Dead Letter Queue
| Topic Name | Purpose |
|------------|---------|
| `cart-service-dlq` | Failed cart events for manual processing |

---

## Payment Service Topics

### Payment Event Topics
| Topic Name | Event Type | Producer | Consumer | Purpose |
|------------|------------|----------|----------|---------|
| `restaurant-events-paymentinitiated` | PaymentInitiated | payment-service | analytics | Payment initiated |
| `restaurant-events-paymentcompleted` | PaymentCompleted | payment-service | order-service, analytics | Payment successful |
| `restaurant-events-paymentfailed` | PaymentFailed | payment-service | order-service, analytics | Payment failed |

### Dead Letter Queue
| Topic Name | Purpose |
|------------|---------|
| `payment-service-dlq` | Failed payment events for manual processing |

---

## User Service Topics

### User Event Topics
| Topic Name | Event Type | Producer | Consumer | Purpose |
|------------|------------|----------|----------|---------|
| `user-events` | UserCreated | user-service | notification, analytics | New user registered |
| `user-events` | UserUpdated | user-service | analytics | User profile updated |

---

## Restaurant Service Topics

### Restaurant Event Topics
| Topic Name | Event Type | Producer | Consumer | Purpose |
|------------|------------|----------|----------|---------|
| `restaurant-events` | RestaurantCreated | restaurant-service | analytics | New restaurant added |
| `restaurant-events` | MenuUpdated | restaurant-service | cart-service, analytics | Menu items updated |

---

## Topic Naming Convention

### Pattern
```
{service-prefix}-{event-type-lowercase}
```

### Examples
- Order events: `restaurant-events-ordercreated`
- Cart events: `restaurant-events-cartitemadded`
- User events: `user-events`
- Saga events: `order-saga-started`

### Dead Letter Queue Pattern
```
{service-name}-dlq
```

### Examples
- `order-service-dlq`
- `cart-service-dlq`
- `payment-service-dlq`

---

## Topic Configuration Requirements

### Kafka Topic Creation Commands

```bash
# Order Service Topics
kafka-topics.sh --create --topic order-saga-started --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic cart-validation-requested --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic cart-validation-completed --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic payment-initiation-requested --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic payment-processing-completed --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic order-confirmed --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic order-cancelled --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic restaurant-events-ordercreated --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic restaurant-events-orderconfirmed --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic restaurant-events-ordercancelled --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic order-service-dlq --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# Cart Service Topics
kafka-topics.sh --create --topic restaurant-events-cartitemadded --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic restaurant-events-cartitemremoved --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic restaurant-events-cartcleared --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic cart-service-dlq --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# Payment Service Topics
kafka-topics.sh --create --topic restaurant-events-paymentinitiated --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic restaurant-events-paymentcompleted --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic restaurant-events-paymentfailed --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
kafka-topics.sh --create --topic payment-service-dlq --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1

# User Service Topics
kafka-topics.sh --create --topic user-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Restaurant Service Topics
kafka-topics.sh --create --topic restaurant-events --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

---

## Quick Reference - One Line Per Topic

```
order-saga-started | OrderCreated | Saga orchestration start
cart-validation-requested | CartValidationRequested | Request cart validation
cart-validation-completed | CartValidationCompleted | Cart validation result
payment-initiation-requested | PaymentInitiationRequested | Request payment processing
payment-processing-completed | PaymentProcessingCompleted | Payment processing result
order-confirmed | OrderConfirmed | Order successfully confirmed
order-cancelled | OrderCancelled | Order cancelled or failed
restaurant-events-ordercreated | OrderCreated | New order created event
restaurant-events-orderconfirmed | OrderConfirmed | Order confirmed event
restaurant-events-ordercancelled | OrderCancelled | Order cancelled event
restaurant-events-cartitemadded | CartItemAdded | Item added to cart
restaurant-events-cartitemremoved | CartItemRemoved | Item removed from cart
restaurant-events-cartcleared | CartCleared | Cart cleared
restaurant-events-paymentinitiated | PaymentInitiated | Payment initiated
restaurant-events-paymentcompleted | PaymentCompleted | Payment completed successfully
restaurant-events-paymentfailed | PaymentFailed | Payment failed
user-events | UserCreated/UserUpdated | User service events
restaurant-events | RestaurantCreated/MenuUpdated | Restaurant service events
order-service-dlq | Dead letter queue for order service
cart-service-dlq | Dead letter queue for cart service
payment-service-dlq | Dead letter queue for payment service
```

---

## Total Topics: 21
- Saga Topics: 7
- Order Event Topics: 3
- Cart Event Topics: 3
- Payment Event Topics: 3
- User Event Topics: 1
- Restaurant Event Topics: 1
- Dead Letter Queues: 3
