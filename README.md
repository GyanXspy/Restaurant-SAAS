# Restaurant Food Ordering System

A microservices architecture for restaurant food ordering using Spring Boot and event-driven patterns including Saga Orchestration, CQRS, and Event Sourcing.

## Architecture Overview

The system consists of 5 microservices:

- **User Service** - User registration, authentication, and profile management
- **Restaurant Service** - Restaurant and menu management
- **Cart Service** - Shopping cart management and validation
- **Order Service** - Order processing with Saga orchestration
- **Payment Service** - Payment processing

## Technology Stack

- **Framework**: Spring Boot 3.2.0
- **Build Tool**: Maven 3.9+
- **Java Version**: 17+

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.9 or higher

### 1. Build and Start All Services

```batch
# Windows - Run the startup script
start-services.bat
```

This will:
1. Build all services
2. Start each service in its own command window
3. Services will be available on ports 8081-8085

### 2. Manual Service Startup

Each service can be run independently:

```batch
# User Service (Port 8081)
cd user-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# Restaurant Service (Port 8082)
cd restaurant-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082

# Cart Service (Port 8083)
cd cart-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083

# Order Service (Port 8084)
cd order-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8084

# Payment Service (Port 8085)
cd payment-service
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8085
```

## Service Endpoints

- **User Service**: http://localhost:8081
- **Restaurant Service**: http://localhost:8082
- **Cart Service**: http://localhost:8083
- **Order Service**: http://localhost:8084
- **Payment Service**: http://localhost:8085

## Project Structure

```
restaurant-food-ordering-system/
├── shared-events/              # Common event classes
├── user-service/              # User management service
├── restaurant-service/        # Restaurant & menu service
├── cart-service/             # Shopping cart service
├── order-service/            # Order processing & saga orchestration
├── payment-service/          # Payment processing service
├── start-services.bat        # Windows startup script
└── pom.xml                   # Parent POM
```

## Development

### Running Tests

```batch
# Run all tests
mvn test

# Run tests for specific service
mvn test -pl user-service
```

### Building

```batch
# Build all modules
mvn clean install

# Build specific service
mvn clean install -pl user-service
```

### Health Checks

Each service exposes actuator endpoints for monitoring:

- Health: `http://localhost:808X/actuator/health`
- Metrics: `http://localhost:808X/actuator/metrics`
- Info: `http://localhost:808X/actuator/info`

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.