# Restaurant Food Ordering System - Deployment Guide

This guide covers the deployment of the Restaurant Food Ordering System using Docker containers.

## Prerequisites

- Docker 20.0+ and Docker Compose 2.0+
- At least 8GB RAM available for Docker
- Ports 8080-8091 and 3306-3307, 6379, 9092, 27017 available

## Quick Start (Local Development)

```bash
# Clone the repository
git clone <repository-url>
cd restaurant-food-ordering-system

# Quick development setup
make dev-setup
```

This will:
1. Clean any existing Docker resources
2. Build all Docker images
3. Deploy the complete system
4. Show service URLs

## Manual Deployment

### 1. Build Docker Images

```bash
# Build all service images
make build-images

# Or build individually
docker-compose -f docker-compose-services.yml build user-service
docker-compose -f docker-compose-services.yml build restaurant-service
# ... etc
```

### 2. Deploy to Different Environments

#### Local Development (Docker)
```bash
# Using deployment script
./deploy.sh -e docker

# Or using Makefile
make deploy-docker
```

#### Staging Environment
```bash
# Using deployment script
./deploy.sh -e staging

# Or using Makefile
make deploy-staging
```

#### Production Environment
```bash
# First, create production environment file
cp .env.template .env.prod
# Edit .env.prod with production values

# Deploy to production
./deploy.sh -e prod

# Or using Makefile (with confirmation prompt)
make deploy-prod
```

## Environment Configuration

### Environment Files

- `.env.template` - Template with all configuration options
- `.env.staging` - Staging environment configuration (included)
- `.env.prod` - Production environment configuration (create from template)

### Key Configuration Areas

#### Database Configuration
```bash
# MySQL (Order and Payment Services)
MYSQL_ROOT_PASSWORD=your_secure_root_password
MYSQL_USER=restaurant_user
MYSQL_PASSWORD=your_secure_password

# MongoDB (User, Restaurant, Cart Services)
MONGO_ROOT_USERNAME=admin
MONGO_ROOT_PASSWORD=your_secure_mongo_password
```

#### Kafka Configuration
```bash
KAFKA_BOOTSTRAP_SERVERS=kafka:29092
```

#### Application Profiles
```bash
# Available profiles: docker, staging, prod
SPRING_PROFILES_ACTIVE=prod
```

## Service Architecture

### Microservices
- **User Service** (Port 8081) - User management with MongoDB
- **Restaurant Service** (Port 8082) - Restaurant and menu management with MongoDB
- **Cart Service** (Port 8083) - Shopping cart management with MongoDB
- **Order Service** (Port 8084) - Order processing and Saga orchestration with MySQL
- **Payment Service** (Port 8085) - Payment processing with MySQL

### Infrastructure Services
- **Kafka** (Port 9092) - Event streaming
- **MySQL** (Port 3306) - Order service database
- **MySQL Payment** (Port 3307) - Payment service database
- **MongoDB** (Port 27017) - User, Restaurant, Cart services database
- **Redis** (Port 6379) - Caching layer

### Management Tools (Development Only)
- **Kafka UI** (Port 8080) - Kafka management interface
- **Adminer** (Port 8090) - MySQL database management
- **Mongo Express** (Port 8091) - MongoDB management interface

## Deployment Options

### Option 1: Complete System Deployment

Deploy all services together:

```bash
# Development
./deploy.sh -e docker

# Staging
./deploy.sh -e staging

# Production
./deploy.sh -e prod
```

### Option 2: Infrastructure Only

Start only infrastructure services for local development:

```bash
make start-infra
```

Then start individual services locally:

```bash
make start-user
make start-restaurant
# etc.
```

### Option 3: Custom Deployment

Use Docker Compose directly:

```bash
# Start infrastructure
docker-compose -f docker-compose-services.yml up -d zookeeper kafka mysql mysql-payment mongodb redis

# Start specific services
docker-compose -f docker-compose-services.yml up -d user-service restaurant-service

# With environment overrides
docker-compose -f docker-compose-services.yml -f docker-compose.prod.yml up -d
```

## Deployment Script Options

The `deploy.sh` script supports various options:

```bash
./deploy.sh [OPTIONS]

Options:
  -e, --environment ENV    Set environment (docker|staging|prod) [default: docker]
  -n, --no-build          Skip building services
  -p, --no-pull           Skip pulling base images
  -c, --cleanup           Clean up containers and volumes before deployment
  -h, --help              Show help message

Examples:
  ./deploy.sh                      # Deploy with default settings
  ./deploy.sh -e staging           # Deploy to staging
  ./deploy.sh -e prod -n           # Deploy to production without rebuilding
  ./deploy.sh -c                   # Clean deployment
```

## Health Checks and Monitoring

### Health Check URLs

All services expose health check endpoints:

- User Service: http://localhost:8081/actuator/health
- Restaurant Service: http://localhost:8082/actuator/health
- Cart Service: http://localhost:8083/actuator/health
- Order Service: http://localhost:8084/actuator/health
- Payment Service: http://localhost:8085/actuator/health

### Automated Health Checks

```bash
# Check all service health
make health-check

# Monitor service status
make status
```

### Viewing Logs

```bash
# All services
make logs-docker

# Specific service
make logs-docker-user-service
make logs-docker-order-service

# Or using docker-compose directly
docker-compose -f docker-compose-services.yml logs -f user-service
```

## Scaling and Production Considerations

### Resource Requirements

#### Minimum (Development)
- 4 CPU cores
- 8GB RAM
- 20GB disk space

#### Recommended (Production)
- 8+ CPU cores
- 16GB+ RAM
- 100GB+ disk space (with proper volume management)

### Production Scaling

The production configuration (`docker-compose.prod.yml`) includes:

- **Service Replicas**: Multiple instances of each service
- **Resource Limits**: CPU and memory constraints
- **JVM Optimization**: G1GC and optimized heap settings
- **Database Optimization**: Connection pooling and performance tuning

### Security Considerations

1. **Environment Variables**: Use secure passwords and keys
2. **Network Security**: Configure proper firewall rules
3. **Database Security**: Use strong passwords and limit access
4. **SSL/TLS**: Enable SSL for production databases
5. **Secrets Management**: Use Docker secrets or external secret management

## Troubleshooting

### Common Issues

#### Services Not Starting
```bash
# Check service logs
docker-compose -f docker-compose-services.yml logs service-name

# Check resource usage
docker stats

# Restart specific service
docker-compose -f docker-compose-services.yml restart service-name
```

#### Database Connection Issues
```bash
# Check database health
docker-compose -f docker-compose-services.yml exec mysql mysqladmin ping
docker-compose -f docker-compose-services.yml exec mongodb mongosh --eval "db.adminCommand('ping')"

# Check network connectivity
docker-compose -f docker-compose-services.yml exec user-service ping mongodb
```

#### Kafka Issues
```bash
# Check Kafka health
docker-compose -f docker-compose-services.yml exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# List topics
docker-compose -f docker-compose-services.yml exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

### Performance Issues

#### Memory Issues
```bash
# Check memory usage
docker stats

# Adjust JVM settings in environment files
JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
```

#### Database Performance
```bash
# Check MySQL performance
docker-compose -f docker-compose-services.yml exec mysql mysql -u root -p -e "SHOW PROCESSLIST;"

# Check MongoDB performance
docker-compose -f docker-compose-services.yml exec mongodb mongosh --eval "db.currentOp()"
```

## Backup and Recovery

### Database Backups

```bash
# MySQL backup
docker exec mysql mysqldump -u root -prootpassword restaurant_orders > backup_orders.sql
docker exec mysql-payment mysqldump -u root -prootpassword restaurant_payments > backup_payments.sql

# MongoDB backup
docker exec mongodb mongodump --uri="mongodb://admin:adminpassword@localhost:27017/restaurant_app?authSource=admin" --out=/backup
```

### Volume Management

```bash
# List volumes
docker volume ls

# Backup volume
docker run --rm -v restaurant-food-ordering-system_mysql_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql_data.tar.gz /data
```

## Maintenance

### Updates and Upgrades

```bash
# Update base images
docker-compose -f docker-compose-services.yml pull

# Rebuild services
make build-images

# Rolling update (zero-downtime)
docker-compose -f docker-compose-services.yml up -d --no-deps service-name
```

### Cleanup

```bash
# Clean Docker resources
make clean-docker

# Full cleanup (removes volumes)
./deploy.sh -c
```

## Support

For issues and questions:

1. Check the logs first: `make logs-docker`
2. Verify health checks: `make health-check`
3. Check resource usage: `docker stats`
4. Review configuration files and environment variables