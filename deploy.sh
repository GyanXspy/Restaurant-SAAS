#!/bin/bash

# Restaurant Food Ordering System Deployment Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
ENVIRONMENT="docker"
BUILD_SERVICES=true
PULL_IMAGES=true
CLEANUP=false

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Function to show usage
show_usage() {
    echo "Usage: $0 [OPTIONS]"
    echo ""
    echo "Options:"
    echo "  -e, --environment ENV    Set environment (docker|staging|prod) [default: docker]"
    echo "  -n, --no-build          Skip building services"
    echo "  -p, --no-pull           Skip pulling base images"
    echo "  -c, --cleanup           Clean up containers and volumes before deployment"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                      # Deploy with default settings (docker environment)"
    echo "  $0 -e staging           # Deploy to staging environment"
    echo "  $0 -e prod -n           # Deploy to production without rebuilding"
    echo "  $0 -c                   # Clean deployment (removes existing containers/volumes)"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -e|--environment)
            ENVIRONMENT="$2"
            shift 2
            ;;
        -n|--no-build)
            BUILD_SERVICES=false
            shift
            ;;
        -p|--no-pull)
            PULL_IMAGES=false
            shift
            ;;
        -c|--cleanup)
            CLEANUP=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Validate environment
if [[ ! "$ENVIRONMENT" =~ ^(docker|staging|prod)$ ]]; then
    print_error "Invalid environment: $ENVIRONMENT. Must be one of: docker, staging, prod"
    exit 1
fi

print_status "Starting deployment for environment: $ENVIRONMENT"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker and try again."
    exit 1
fi

# Check if Docker Compose is available
if ! command -v docker-compose > /dev/null 2>&1; then
    print_error "Docker Compose is not installed. Please install Docker Compose and try again."
    exit 1
fi

# Set compose files based on environment
COMPOSE_FILES="-f docker-compose-services.yml"
case $ENVIRONMENT in
    staging)
        COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.staging.yml"
        ;;
    prod)
        COMPOSE_FILES="$COMPOSE_FILES -f docker-compose.prod.yml"
        if [[ ! -f ".env.prod" ]]; then
            print_error "Production environment file .env.prod not found!"
            print_warning "Please create .env.prod with production configuration"
            exit 1
        fi
        export $(cat .env.prod | xargs)
        ;;
esac

# Cleanup if requested
if [[ "$CLEANUP" == true ]]; then
    print_warning "Cleaning up existing containers and volumes..."
    docker-compose $COMPOSE_FILES down -v --remove-orphans || true
    docker system prune -f || true
fi

# Pull base images if requested
if [[ "$PULL_IMAGES" == true ]]; then
    print_status "Pulling base images..."
    docker-compose $COMPOSE_FILES pull
fi

# Build services if requested
if [[ "$BUILD_SERVICES" == true ]]; then
    print_status "Building microservices..."
    docker-compose $COMPOSE_FILES build --parallel
fi

# Start infrastructure services first
print_status "Starting infrastructure services..."
docker-compose $COMPOSE_FILES up -d zookeeper kafka mysql mysql-payment mongodb redis

# Wait for infrastructure to be ready
print_status "Waiting for infrastructure services to be ready..."
sleep 30

# Check infrastructure health
print_status "Checking infrastructure health..."
for service in kafka mysql mysql-payment mongodb redis; do
    if ! docker-compose $COMPOSE_FILES ps $service | grep -q "Up"; then
        print_error "Service $service is not running properly"
        docker-compose $COMPOSE_FILES logs $service
        exit 1
    fi
done

# Start microservices
print_status "Starting microservices..."
docker-compose $COMPOSE_FILES up -d user-service restaurant-service cart-service payment-service order-service

# Wait for services to start
print_status "Waiting for microservices to start..."
sleep 60

# Check service health
print_status "Checking microservice health..."
services=("user-service" "restaurant-service" "cart-service" "payment-service" "order-service")
for service in "${services[@]}"; do
    port=""
    case $service in
        user-service) port="8081" ;;
        restaurant-service) port="8082" ;;
        cart-service) port="8083" ;;
        order-service) port="8084" ;;
        payment-service) port="8085" ;;
    esac
    
    # Wait for health check
    max_attempts=30
    attempt=1
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
            print_status "$service is healthy"
            break
        fi
        
        if [[ $attempt -eq $max_attempts ]]; then
            print_error "$service health check failed after $max_attempts attempts"
            docker-compose $COMPOSE_FILES logs $service
            exit 1
        fi
        
        print_warning "$service not ready yet (attempt $attempt/$max_attempts), waiting..."
        sleep 10
        ((attempt++))
    done
done

# Start management tools for non-production environments
if [[ "$ENVIRONMENT" != "prod" ]]; then
    print_status "Starting management tools..."
    docker-compose $COMPOSE_FILES up -d kafka-ui adminer mongo-express
fi

print_status "Deployment completed successfully!"
print_status ""
print_status "Service URLs:"
print_status "  User Service:       http://localhost:8081"
print_status "  Restaurant Service: http://localhost:8082"
print_status "  Cart Service:       http://localhost:8083"
print_status "  Order Service:      http://localhost:8084"
print_status "  Payment Service:    http://localhost:8085"

if [[ "$ENVIRONMENT" != "prod" ]]; then
    print_status ""
    print_status "Management Tools:"
    print_status "  Kafka UI:           http://localhost:8080"
    print_status "  Adminer (MySQL):    http://localhost:8090"
    print_status "  Mongo Express:      http://localhost:8091"
fi

print_status ""
print_status "Health Check URLs:"
for service in "${services[@]}"; do
    port=""
    case $service in
        user-service) port="8081" ;;
        restaurant-service) port="8082" ;;
        cart-service) port="8083" ;;
        order-service) port="8084" ;;
        payment-service) port="8085" ;;
    esac
    print_status "  $service: http://localhost:$port/actuator/health"
done

print_status ""
print_status "To view logs: docker-compose $COMPOSE_FILES logs -f [service-name]"
print_status "To stop all services: docker-compose $COMPOSE_FILES down"