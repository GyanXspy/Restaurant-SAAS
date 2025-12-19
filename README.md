# Restaurant Food Ordering System

A high-scale, event-driven microservices architecture for restaurant food ordering using Spring Boot, Apache Kafka, and advanced patterns including Saga Orchestration, CQRS, and Event Sourcing.

## Architecture Overview

The system consists of 5 microservices:

- **User Service** - User registration, authentication, and profile management (MongoDB + CQRS)
- **Restaurant Service** - Restaurant and menu management (MongoDB + Event Publishing)
- **Cart Service** - Shopping cart management and validation (MongoDB + Event Handling)
- **Order Service** - Order processing with Saga orchestration (MySQL + CQRS + Event Sourcing)
- **Payment Service** - Payment processing (MySQL + Event Sourcing)

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Message Broker**: Apache Kafka 3.6.0
- **Databases**: MySQL 8.0, MongoDB 7.0
- **Caching**: Redis 7.2
- **Build Tool**: Maven 3.9+
- **Java Version**: 17+
- **Containerization**: Docker & Docker Compose

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.9 or higher
- Docker and Docker Compose

### 1. Start Infrastructure Services

```bash
# Start all infrastructure services (Kafka, MySQL, MongoDB, Redis)
docker-compose up -d

# Verify services are running
docker-compose ps
```

### 2. Build the Project

```bash
# Build all modules
mvn clean install

# Or build individual services
mvn clean install -pl shared-events
mvn clean install -pl user-service
mvn clean install -pl restaurant-service
mvn clean install -pl cart-service
mvn clean install -pl order-service
mvn clean install -pl payment-service
```

### 3. Run Services

Each service can be run independently:

```bash
# User Service (Port 8081)
cd user-service && mvn spring-boot:run

# Restaurant Service (Port 8082)
cd restaurant-service && mvn spring-boot:run

# Cart Service (Port 8083)
cd cart-service && mvn spring-boot:run

# Order Service (Port 8084)
cd order-service && mvn spring-boot:run

# Payment Service (Port 8085)
cd payment-service && mvn spring-boot:run
```

## Infrastructure Services

### Kafka
- **Broker**: localhost:9092
- **UI**: http://localhost:8080 (Kafka UI)

### Databases
- **MySQL (Orders)**: localhost:3306
  - Database: `restaurant_orders`
  - User: `restaurant_user`
  - Password: `restaurant_password`

- **MySQL (Payments)**: localhost:3307
  - Database: `restaurant_payments`
  - User: `payment_user`
  - Password: `payment_password`

- **MongoDB**: localhost:27017
  - Database: `restaurant_app`
  - User: `admin`
  - Password: `adminpassword`

### Management UIs
- **Kafka UI**: http://localhost:8080
- **Adminer (MySQL)**: http://localhost:8081
- **Mongo Express**: http://localhost:8082
- **Redis**: localhost:6379

## Project Structure

```
restaurant-food-ordering-system/
├── shared-events/              # Common event classes
├── user-service/              # User management service
├── restaurant-service/        # Restaurant & menu service
├── cart-service/             # Shopping cart service
├── order-service/            # Order processing & saga orchestration
├── payment-service/          # Payment processing service
├── docker-compose.yml        # Infrastructure services
└── pom.xml                   # Parent POM
```

## Event Flow

### Order Processing Saga

1. **Order Created** → `order-saga-started`
2. **Cart Validation** → `cart-validation-requested` → `cart-validation-completed`
3. **Payment Processing** → `payment-initiation-requested` → `payment-processing-completed`
4. **Order Confirmation** → `order-confirmed` or `order-cancelled`

### Kafka Topics

- `order-saga-started` - Saga initiation events
- `cart-validation-requested` - Cart validation requests
- `cart-validation-completed` - Cart validation responses
- `payment-initiation-requested` - Payment processing requests
- `payment-processing-completed` - Payment processing responses
- `order-confirmed` - Successful order confirmations
- `order-cancelled` - Order cancellation events

## Development

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for specific service
mvn test -pl user-service
```

### Code Quality

```bash
# Run static analysis
mvn compile

# Check dependencies
mvn dependency:tree
```

### Debugging

Each service exposes actuator endpoints for monitoring:

- Health: `http://localhost:808X/actuator/health`
- Metrics: `http://localhost:808X/actuator/metrics`
- Info: `http://localhost:808X/actuator/info`

## Patterns Implemented

### Event Sourcing
- All state changes persisted as immutable events
- Event replay for aggregate reconstruction
- Audit trail for all business operations

### CQRS (Command Query Responsibility Segregation)
- Separate command and query models
- Optimized read models for different query patterns
- Asynchronous read model updates via events

### Saga Orchestration
- Centralized saga orchestrator in Order Service
- Compensating actions for failure scenarios
- Timeout handling and retry mechanisms

### Event-Driven Architecture
- Asynchronous communication via Kafka
- Idempotent event processing
- Dead letter queue for failed events

## Monitoring and Observability

- **Health Checks**: Spring Boot Actuator
- **Metrics**: Custom business and technical metrics
- **Logging**: Structured logging with correlation IDs
- **Tracing**: Distributed tracing across services

## Scalability Features

- **Horizontal Scaling**: Stateless services
- **Database Scaling**: Read replicas and sharding
- **Kafka Scaling**: Partitioned topics for load distribution
- **Caching**: Redis for frequently accessed data

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.