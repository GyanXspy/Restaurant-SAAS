# Integration Tests

This module contains comprehensive end-to-end integration tests for the Restaurant Food Ordering System.

## Test Coverage

### 1. OrderFlowIntegrationTest
- Tests complete order processing flow across all microservices
- Verifies saga orchestration behavior
- Tests successful order completion scenarios
- Validates event-driven communication between services

### 2. SagaCompensationIntegrationTest
- Tests saga compensation scenarios
- Verifies failure handling and rollback mechanisms
- Tests timeout scenarios
- Validates compensating actions for different failure points

### 3. EventSourcingCQRSIntegrationTest
- Tests event sourcing behavior and aggregate reconstruction
- Verifies CQRS pattern implementation
- Tests read model updates and eventual consistency
- Validates event versioning and point-in-time reconstruction

### 4. PerformanceIntegrationTest
- Tests system behavior under high load
- Verifies concurrent order processing
- Tests event ordering under load
- Validates system recovery from temporary failures

## Infrastructure

### Testcontainers
The tests use Testcontainers to provide:
- **Kafka**: Event streaming and messaging
- **MySQL**: Persistent storage for Order and Payment services
- **MongoDB**: Document storage for User, Restaurant, and Cart services

### Test Configuration
- All services run in a single Spring Boot context for testing
- Separate test profiles and configurations
- Automatic container lifecycle management
- Dynamic property configuration based on container ports

## Running the Tests

### Prerequisites
- Docker installed and running
- Java 17+
- Maven 3.6+

### Run All Integration Tests
```bash
mvn test -pl integration-tests
```

### Run Specific Test Class
```bash
mvn test -pl integration-tests -Dtest=OrderFlowIntegrationTest
```

### Run Test Suite
```bash
mvn test -pl integration-tests -Dtest=IntegrationTestSuite
```

## Test Data Management

### TestDataBuilder
Provides consistent test data creation across all tests:
- User test data
- Restaurant and menu test data
- Order test data
- Payment test data

### Event Capture
Tests use event capture mechanisms to verify:
- Event publishing and consumption
- Saga progression
- Compensation scenarios
- Event ordering and versioning

## Configuration

### Application Properties
- `application-integration-test.yml`: Test-specific configuration
- Dynamic properties from Testcontainers
- Kafka topic configuration
- Database connection settings

### Performance Tuning
- Connection pool settings optimized for testing
- Kafka consumer/producer configurations
- Timeout settings for async operations
- Retry mechanisms for transient failures

## Troubleshooting

### Common Issues

1. **Container Startup Failures**
   - Ensure Docker is running
   - Check available memory (containers need ~2GB)
   - Verify no port conflicts

2. **Test Timeouts**
   - Increase Awaitility timeout values
   - Check container resource allocation
   - Verify Kafka topic creation

3. **Event Ordering Issues**
   - Ensure proper partition key usage
   - Check Kafka consumer configuration
   - Verify event versioning

### Debug Mode
Enable debug logging by setting:
```yaml
logging:
  level:
    com.restaurant: DEBUG
    org.springframework.kafka: DEBUG
```

## Continuous Integration

### CI Configuration
The tests are designed to run in CI environments:
- Testcontainers with Docker-in-Docker support
- Parallel execution disabled for resource management
- Proper cleanup and resource management
- Fail-fast configuration for quick feedback

### Resource Requirements
- Memory: 4GB minimum, 8GB recommended
- CPU: 2 cores minimum
- Disk: 10GB for container images and data
- Network: Internet access for container image downloads