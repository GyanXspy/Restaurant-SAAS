# Multi-stage build for User Service
FROM maven:3.9.4-openjdk-17-slim AS build

# Set working directory
WORKDIR /app

# Copy parent pom and shared-events first for better layer caching
COPY pom.xml .
COPY shared-events/pom.xml ./shared-events/
COPY shared-events/src ./shared-events/src

# Copy user-service pom and source
COPY user-service/pom.xml ./user-service/
COPY user-service/src ./user-service/src

# Build the application
RUN mvn clean package -pl user-service -am -DskipTests

# Runtime stage
FROM openjdk:17-jre-slim

# Create app user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/user-service/target/user-service-*.jar app.jar

# Change ownership to app user
RUN chown -R appuser:appuser /app
USER appuser

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]