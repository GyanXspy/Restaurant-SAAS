# Multi-stage build for Order Service
FROM maven:3.9.4-openjdk-17-slim AS build

# Set working directory
WORKDIR /app

# Copy parent pom and shared-events first for better layer caching
COPY pom.xml .
COPY shared-events/pom.xml ./shared-events/
COPY shared-events/src ./shared-events/src

# Copy order-service pom and source
COPY order-service/pom.xml ./order-service/
COPY order-service/src ./order-service/src

# Build the application
RUN mvn clean package -pl order-service -am -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

# Create app user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/order-service/target/order-service-*.jar app.jar

# Change ownership to app user
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8084/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]