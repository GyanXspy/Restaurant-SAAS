#!/bin/bash

# Restaurant Food Ordering System - Monitoring Deployment Script
set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

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
    echo "  -s, --services-only     Deploy only monitoring services (no application services)"
    echo "  -a, --all              Deploy both application and monitoring services"
    echo "  -c, --cleanup          Clean up existing monitoring containers"
    echo "  -h, --help             Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 -s                  # Deploy only monitoring stack"
    echo "  $0 -a                  # Deploy complete system with monitoring"
    echo "  $0 -c                  # Clean deployment"
}

# Default values
SERVICES_ONLY=false
DEPLOY_ALL=false
CLEANUP=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--services-only)
            SERVICES_ONLY=true
            shift
            ;;
        -a|--all)
            DEPLOY_ALL=true
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

print_status "Starting monitoring deployment..."

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

# Create necessary directories
print_status "Creating monitoring directories..."
mkdir -p monitoring/grafana/provisioning/datasources
mkdir -p monitoring/grafana/provisioning/dashboards
mkdir -p monitoring/grafana/dashboards
mkdir -p logs

# Cleanup if requested
if [[ "$CLEANUP" == true ]]; then
    print_warning "Cleaning up existing monitoring containers..."
    docker-compose -f docker-compose.monitoring.yml down -v --remove-orphans || true
    if [[ "$DEPLOY_ALL" == true ]]; then
        docker-compose -f docker-compose-services.yml down -v --remove-orphans || true
    fi
    docker system prune -f || true
fi

# Create network if it doesn't exist
print_status "Creating Docker network..."
docker network create restaurant-network 2>/dev/null || true

# Deploy application services if requested
if [[ "$DEPLOY_ALL" == true ]]; then
    print_status "Deploying application services..."
    docker-compose -f docker-compose-services.yml up -d
    
    print_status "Waiting for application services to be ready..."
    sleep 60
    
    # Check application service health
    services=("user-service" "restaurant-service" "cart-service" "order-service" "payment-service")
    for service in "${services[@]}"; do
        port=""
        case $service in
            user-service) port="8081" ;;
            restaurant-service) port="8082" ;;
            cart-service) port="8083" ;;
            order-service) port="8084" ;;
            payment-service) port="8085" ;;
        esac
        
        max_attempts=20
        attempt=1
        while [[ $attempt -le $max_attempts ]]; do
            if curl -f -s "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
                print_status "$service is healthy"
                break
            fi
            
            if [[ $attempt -eq $max_attempts ]]; then
                print_warning "$service health check failed, but continuing with monitoring deployment"
                break
            fi
            
            print_warning "$service not ready yet (attempt $attempt/$max_attempts), waiting..."
            sleep 5
            ((attempt++))
        done
    done
fi

# Deploy monitoring services
print_status "Deploying monitoring services..."
docker-compose -f docker-compose.monitoring.yml up -d

print_status "Waiting for monitoring services to be ready..."
sleep 30

# Check monitoring service health
monitoring_services=("prometheus" "grafana" "alertmanager" "loki")
for service in "${monitoring_services[@]}"; do
    port=""
    case $service in
        prometheus) port="9090" ;;
        grafana) port="3000" ;;
        alertmanager) port="9093" ;;
        loki) port="3100" ;;
    esac
    
    max_attempts=20
    attempt=1
    while [[ $attempt -le $max_attempts ]]; do
        if curl -f -s "http://localhost:$port" > /dev/null 2>&1; then
            print_status "$service is healthy"
            break
        fi
        
        if [[ $attempt -eq $max_attempts ]]; then
            print_error "$service health check failed after $max_attempts attempts"
            docker-compose -f docker-compose.monitoring.yml logs $service
            exit 1
        fi
        
        print_warning "$service not ready yet (attempt $attempt/$max_attempts), waiting..."
        sleep 5
        ((attempt++))
    done
done

print_status "Monitoring deployment completed successfully!"
print_status ""
print_status "Monitoring Services:"
print_status "  Prometheus:         http://localhost:9090"
print_status "  Grafana:            http://localhost:3000 (admin/admin123)"
print_status "  AlertManager:       http://localhost:9093"
print_status "  Zipkin:             http://localhost:9411"
print_status "  Jaeger:             http://localhost:16686"
print_status "  Loki:               http://localhost:3100"

if [[ "$DEPLOY_ALL" == true ]]; then
    print_status ""
    print_status "Application Services:"
    print_status "  User Service:       http://localhost:8081"
    print_status "  Restaurant Service: http://localhost:8082"
    print_status "  Cart Service:       http://localhost:8083"
    print_status "  Order Service:      http://localhost:8084"
    print_status "  Payment Service:    http://localhost:8085"
    print_status ""
    print_status "Management Tools:"
    print_status "  Kafka UI:           http://localhost:8080"
    print_status "  Adminer:            http://localhost:8090"
    print_status "  Mongo Express:      http://localhost:8091"
fi

print_status ""
print_status "Infrastructure Monitoring:"
print_status "  Node Exporter:      http://localhost:9100"
print_status "  cAdvisor:           http://localhost:8080"

print_status ""
print_status "Grafana Dashboards:"
print_status "  - Restaurant System Overview"
print_status "  - Business Metrics"
print_status "  - Infrastructure Metrics"

print_status ""
print_status "To view logs:"
print_status "  Monitoring: docker-compose -f docker-compose.monitoring.yml logs -f [service-name]"
if [[ "$DEPLOY_ALL" == true ]]; then
    print_status "  Application: docker-compose -f docker-compose-services.yml logs -f [service-name]"
fi

print_status ""
print_status "To stop services:"
print_status "  Monitoring: docker-compose -f docker-compose.monitoring.yml down"
if [[ "$DEPLOY_ALL" == true ]]; then
    print_status "  Application: docker-compose -f docker-compose-services.yml down"
fi