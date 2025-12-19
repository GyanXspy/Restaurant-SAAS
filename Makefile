# Restaurant Food Ordering System - Development Makefile

.PHONY: help build test clean start-infra stop-infra start-services stop-services logs deploy-docker deploy-staging deploy-prod build-images clean-docker health-check

# Default target
help:
	@echo "Restaurant Food Ordering System - Available Commands:"
	@echo ""
	@echo "Infrastructure:"
	@echo "  start-infra      - Start all infrastructure services (Kafka, MySQL, MongoDB, Redis)"
	@echo "  stop-infra       - Stop all infrastructure services"
	@echo "  logs-infra       - Show infrastructure logs"
	@echo ""
	@echo "Build & Test:"
	@echo "  build            - Build all services with Maven"
	@echo "  build-images     - Build Docker images for all services"
	@echo "  test             - Run all tests"
	@echo "  clean            - Clean all build artifacts"
	@echo "  clean-docker     - Clean Docker containers, images, and volumes"
	@echo ""
	@echo "Docker Deployment:"
	@echo "  deploy-docker    - Deploy complete system for local development"
	@echo "  deploy-staging   - Deploy to staging environment"
	@echo "  deploy-prod      - Deploy to production environment"
	@echo "  health-check     - Check health of all services"
	@echo ""
	@echo "Services (Local Development):"
	@echo "  start-user       - Start User Service (port 8081)"
	@echo "  start-restaurant - Start Restaurant Service (port 8082)"
	@echo "  start-cart       - Start Cart Service (port 8083)"
	@echo "  start-order      - Start Order Service (port 8084)"
	@echo "  start-payment    - Start Payment Service (port 8085)"
	@echo ""
	@echo "Development:"
	@echo "  setup            - Complete setup (infra + build)"
	@echo "  dev-setup        - Quick Docker development setup"
	@echo "  status           - Show service status"

# Infrastructure Management
start-infra:
	@echo "Starting infrastructure services..."
	docker-compose up -d
	@echo "Waiting for services to be ready..."
	@sleep 10
	@echo "Infrastructure services started!"
	@echo "Kafka UI: http://localhost:8080"
	@echo "Adminer (MySQL): http://localhost:8081"
	@echo "Mongo Express: http://localhost:8082"

stop-infra:
	@echo "Stopping infrastructure services..."
	docker-compose down

logs-infra:
	docker-compose logs -f

# Build and Test
build:
	@echo "Building all services..."
	mvn clean install -DskipTests

test:
	@echo "Running all tests..."
	mvn test

clean:
	@echo "Cleaning build artifacts..."
	mvn clean

# Service Management
start-user:
	@echo "Starting User Service on port 8081..."
	cd user-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081"

start-restaurant:
	@echo "Starting Restaurant Service on port 8082..."
	cd restaurant-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8082"

start-cart:
	@echo "Starting Cart Service on port 8083..."
	cd cart-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"

start-order:
	@echo "Starting Order Service on port 8084..."
	cd order-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8084"

start-payment:
	@echo "Starting Payment Service on port 8085..."
	cd payment-service && mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8085"

# Development Helpers
setup: start-infra build
	@echo "Setup complete! Infrastructure is running and services are built."
	@echo "You can now start individual services using make start-<service>"

status:
	@echo "Infrastructure Status:"
	@docker-compose ps
	@echo ""
	@echo "Service Endpoints (when running):"
	@echo "  User Service:       http://localhost:8081/actuator/health"
	@echo "  Restaurant Service: http://localhost:8082/actuator/health"
	@echo "  Cart Service:       http://localhost:8083/actuator/health"
	@echo "  Order Service:      http://localhost:8084/actuator/health"
	@echo "  Payment Service:    http://localhost:8085/actuator/health"
# 
Docker Deployment Commands
build-images:
	@echo "Building Docker images..."
	docker-compose -f docker-compose-services.yml build --parallel

deploy-docker:
	@echo "Deploying to Docker environment..."
	bash deploy.sh -e docker

deploy-staging:
	@echo "Deploying to staging environment..."
	bash deploy.sh -e staging

deploy-prod:
	@echo "Deploying to production environment..."
	@echo "WARNING: This will deploy to production!"
	@read -p "Are you sure? [y/N] " -n 1 -r; \
	if [[ $$REPLY =~ ^[Yy]$$ ]]; then \
		echo; \
		bash deploy.sh -e prod; \
	else \
		echo; \
		echo "Deployment cancelled."; \
	fi

clean-docker:
	@echo "Cleaning Docker resources..."
	docker-compose -f docker-compose-services.yml down -v --remove-orphans || true
	docker system prune -f || true

health-check:
	@echo "Checking service health..."
	@echo "User Service:"
	@curl -f -s http://localhost:8081/actuator/health || echo "❌ User Service not healthy"
	@echo "Restaurant Service:"
	@curl -f -s http://localhost:8082/actuator/health || echo "❌ Restaurant Service not healthy"
	@echo "Cart Service:"
	@curl -f -s http://localhost:8083/actuator/health || echo "❌ Cart Service not healthy"
	@echo "Order Service:"
	@curl -f -s http://localhost:8084/actuator/health || echo "❌ Order Service not healthy"
	@echo "Payment Service:"
	@curl -f -s http://localhost:8085/actuator/health || echo "❌ Payment Service not healthy"

dev-setup: clean-docker build-images deploy-docker
	@echo "Development environment is ready!"
	@echo "Services available at:"
	@echo "  User Service:       http://localhost:8081"
	@echo "  Restaurant Service: http://localhost:8082"
	@echo "  Cart Service:       http://localhost:8083"
	@echo "  Order Service:      http://localhost:8084"
	@echo "  Payment Service:    http://localhost:8085"
	@echo "  Kafka UI:           http://localhost:8080"
	@echo "  Adminer:            http://localhost:8090"
	@echo "  Mongo Express:      http://localhost:8091"

# Docker logs
logs-docker:
	docker-compose -f docker-compose-services.yml logs -f

logs-docker-%:
	docker-compose -f docker-compose-services.yml logs -f $*

# Stop Docker services
stop-docker:
	@echo "Stopping Docker services..."
	docker-compose -f docker-compose-services.yml down# Monito
ring Commands
deploy-monitoring:
	@echo "Deploying monitoring stack..."
	bash deploy-monitoring.sh -s

deploy-full-monitoring:
	@echo "Deploying complete system with monitoring..."
	bash deploy-monitoring.sh -a

stop-monitoring:
	@echo "Stopping monitoring services..."
	docker-compose -f docker-compose.monitoring.yml down

logs-monitoring:
	docker-compose -f docker-compose.monitoring.yml logs -f

logs-monitoring-%:
	docker-compose -f docker-compose.monitoring.yml logs -f $*

# Monitoring health checks
check-prometheus:
	@echo "Checking Prometheus health..."
	@curl -f -s http://localhost:9090/-/healthy || echo "❌ Prometheus not healthy"

check-grafana:
	@echo "Checking Grafana health..."
	@curl -f -s http://localhost:3000/api/health || echo "❌ Grafana not healthy"

check-monitoring: check-prometheus check-grafana
	@echo "✅ Monitoring health check completed"

# Open monitoring dashboards
open-prometheus:
	@echo "Opening Prometheus..."
	@python -m webbrowser http://localhost:9090 2>/dev/null || echo "Please open http://localhost:9090 in your browser"

open-grafana:
	@echo "Opening Grafana..."
	@python -m webbrowser http://localhost:3000 2>/dev/null || echo "Please open http://localhost:3000 in your browser (admin/admin123)"

open-alertmanager:
	@echo "Opening AlertManager..."
	@python -m webbrowser http://localhost:9093 2>/dev/null || echo "Please open http://localhost:9093 in your browser"

# Complete system with monitoring
full-system: clean-docker build-images deploy-full-monitoring
	@echo "Complete system with monitoring is ready!"
	@echo ""
	@echo "Application Services:"
	@echo "  User Service:       http://localhost:8081"
	@echo "  Restaurant Service: http://localhost:8082"
	@echo "  Cart Service:       http://localhost:8083"
	@echo "  Order Service:      http://localhost:8084"
	@echo "  Payment Service:    http://localhost:8085"
	@echo ""
	@echo "Monitoring:"
	@echo "  Prometheus:         http://localhost:9090"
	@echo "  Grafana:            http://localhost:3000 (admin/admin123)"
	@echo "  AlertManager:       http://localhost:9093"
	@echo ""
	@echo "Management Tools:"
	@echo "  Kafka UI:           http://localhost:8080"
	@echo "  Adminer:            http://localhost:8090"
	@echo "  Mongo Express:      http://localhost:8091"