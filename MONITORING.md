# Restaurant Food Ordering System - Monitoring Guide

This guide covers the comprehensive monitoring and observability setup for the Restaurant Food Ordering System.

## Overview

The monitoring stack includes:

- **Prometheus** - Metrics collection and alerting
- **Grafana** - Visualization and dashboards
- **AlertManager** - Alert routing and notification
- **Loki** - Log aggregation
- **Zipkin/Jaeger** - Distributed tracing
- **Node Exporter** - System metrics
- **cAdvisor** - Container metrics

## Quick Start

### Deploy Monitoring Only
```bash
# Deploy just the monitoring stack
make deploy-monitoring

# Or using the script directly
./deploy-monitoring.sh -s
```

### Deploy Complete System with Monitoring
```bash
# Deploy application services + monitoring
make full-system

# Or using the script directly
./deploy-monitoring.sh -a
```

## Monitoring Services

### Prometheus (Port 9090)
- **URL**: http://localhost:9090
- **Purpose**: Metrics collection, storage, and alerting
- **Configuration**: `monitoring/prometheus.yml`

Key features:
- Scrapes metrics from all microservices every 10 seconds
- Collects infrastructure metrics from Node Exporter and cAdvisor
- Evaluates alert rules every 15 seconds
- Stores metrics with 200h retention

### Grafana (Port 3000)
- **URL**: http://localhost:3000
- **Credentials**: admin/admin123
- **Purpose**: Visualization and dashboards

Pre-configured dashboards:
- **Restaurant System Overview** - Service health, performance metrics
- **Business Metrics** - Order processing, payment success rates
- **Infrastructure Metrics** - CPU, memory, disk usage

### AlertManager (Port 9093)
- **URL**: http://localhost:9093
- **Purpose**: Alert routing and notification
- **Configuration**: `monitoring/alertmanager.yml`

### Loki (Port 3100)
- **URL**: http://localhost:3100
- **Purpose**: Log aggregation and querying
- **Configuration**: `monitoring/loki-config.yml`

### Zipkin (Port 9411)
- **URL**: http://localhost:9411
- **Purpose**: Distributed tracing
- **Integration**: Spring Boot Sleuth

### Jaeger (Port 16686)
- **URL**: http://localhost:16686
- **Purpose**: Alternative distributed tracing UI

## Metrics and Monitoring

### Application Metrics

#### Business Metrics
```promql
# Order processing metrics
orders_created_total
orders_confirmed_total
orders_cancelled_total

# Payment metrics
payments_initiated_total
payments_completed_total
payments_failed_total

# Cart validation metrics
cart_validations_requested_total
cart_validations_completed_total
cart_validations_failed_total

# Processing duration metrics
saga_processing_duration_seconds
order_processing_duration_seconds
payment_processing_duration_seconds
```

#### Technical Metrics
```promql
# HTTP request metrics
http_server_requests_seconds_count
http_server_requests_seconds_sum
http_server_requests_seconds_bucket

# JVM metrics
jvm_memory_used_bytes
jvm_gc_pause_seconds
jvm_threads_live_threads

# Database metrics
hikaricp_connections_active
hikaricp_connections_max
hikaricp_connections_usage_seconds

# Kafka metrics
kafka_consumer_lag_sum
kafka_producer_record_send_total
```

### Infrastructure Metrics

#### System Metrics (Node Exporter)
```promql
# CPU usage
node_cpu_seconds_total

# Memory usage
node_memory_MemTotal_bytes
node_memory_MemAvailable_bytes

# Disk usage
node_filesystem_size_bytes
node_filesystem_avail_bytes

# Network metrics
node_network_receive_bytes_total
node_network_transmit_bytes_total
```

#### Container Metrics (cAdvisor)
```promql
# Container CPU usage
container_cpu_usage_seconds_total

# Container memory usage
container_memory_usage_bytes
container_memory_limit_bytes

# Container network metrics
container_network_receive_bytes_total
container_network_transmit_bytes_total
```

## Alerting

### Alert Categories

#### Critical Alerts
- **ServiceDown** - Service is not responding
- **LowOrderSuccessRate** - Order success rate below 80%
- **HighPaymentFailureRate** - Payment failure rate above 10%
- **DiskSpaceLow** - Disk space below 10%

#### Warning Alerts
- **HighErrorRate** - HTTP error rate above 5%
- **HighResponseTime** - 95th percentile response time above 2s
- **HighOrderCancellationRate** - Order cancellation rate above 20%
- **LongSagaProcessingTime** - Saga processing time above 60s
- **HighMemoryUsage** - Memory usage above 90%
- **HighCPUUsage** - CPU usage above 80%

### Alert Configuration

Alerts are defined in `monitoring/alert_rules.yml` and include:
- **Expression** - PromQL query defining the alert condition
- **Duration** - How long the condition must persist
- **Severity** - Critical, warning, or info
- **Annotations** - Human-readable description

### Alert Routing

AlertManager routes alerts based on:
- **Severity** - Critical alerts to on-call, warnings to team chat
- **Service** - Different teams for different services
- **Time** - Different routing during business hours vs. off-hours

## Logging

### Structured Logging

All services use structured JSON logging with:
- **Timestamp** - ISO 8601 format
- **Log Level** - ERROR, WARN, INFO, DEBUG
- **Logger Name** - Class or component name
- **Correlation ID** - For request tracing
- **Message** - Log message
- **MDC Context** - Additional context data

### Log Aggregation

Logs are collected by Promtail and sent to Loki:
- **Container Logs** - Automatically collected from Docker containers
- **Application Logs** - Structured logs from services
- **System Logs** - OS-level logs

### Log Queries

Example LogQL queries:
```logql
# All logs from order-service
{job="order-service"}

# Error logs across all services
{job=~".*-service"} |= "ERROR"

# Logs for specific correlation ID
{job=~".*-service"} |= "correlation-id-123"

# Saga processing logs
{job="order-service"} |= "saga"
```

## Distributed Tracing

### Trace Collection

Traces are automatically generated for:
- **HTTP Requests** - Incoming REST API calls
- **Kafka Events** - Event publishing and consumption
- **Database Operations** - JPA/MongoDB operations
- **External Calls** - Service-to-service communication

### Trace Analysis

Use Zipkin or Jaeger to:
- **View Request Flow** - See how requests flow through services
- **Identify Bottlenecks** - Find slow operations
- **Debug Errors** - Trace error propagation
- **Analyze Dependencies** - Understand service interactions

## Dashboards

### Restaurant System Overview Dashboard

Displays:
- **Service Health** - Up/down status of all services
- **Request Rate** - HTTP requests per second
- **Response Times** - Latency percentiles
- **Error Rates** - HTTP error percentages
- **Resource Usage** - CPU and memory consumption

### Business Metrics Dashboard

Shows:
- **Order Metrics** - Creation, confirmation, cancellation rates
- **Payment Metrics** - Success and failure rates
- **Processing Times** - Saga and order processing durations
- **Success Rates** - Overall system success metrics

### Infrastructure Dashboard

Monitors:
- **System Resources** - CPU, memory, disk, network
- **Container Metrics** - Docker container performance
- **Database Performance** - Connection pools, query times
- **Kafka Metrics** - Message throughput, consumer lag

## Operational Procedures

### Daily Monitoring Tasks

1. **Check Service Health** - Verify all services are up
2. **Review Error Rates** - Look for unusual error patterns
3. **Monitor Resource Usage** - Check for resource constraints
4. **Validate Business Metrics** - Ensure order processing is normal

### Weekly Monitoring Tasks

1. **Review Alert History** - Analyze alert patterns
2. **Check Disk Usage** - Ensure adequate storage
3. **Update Dashboards** - Add new metrics as needed
4. **Review Performance Trends** - Identify degradation

### Incident Response

1. **Alert Notification** - Receive alert via configured channels
2. **Initial Assessment** - Check dashboards for context
3. **Log Analysis** - Search logs for error details
4. **Trace Analysis** - Use distributed tracing for request flow
5. **Resolution** - Fix issue and verify recovery
6. **Post-Incident** - Update alerts/dashboards as needed

## Troubleshooting

### Common Issues

#### High Memory Usage
```bash
# Check container memory usage
docker stats

# Check JVM heap usage in Grafana
# Look for memory leaks in application logs
```

#### High Response Times
```bash
# Check database connection pools
# Review slow query logs
# Analyze distributed traces in Zipkin
```

#### Alert Fatigue
```bash
# Review alert thresholds
# Adjust alert sensitivity
# Add alert dependencies
```

### Monitoring the Monitoring

Monitor the monitoring stack itself:
- **Prometheus Targets** - Ensure all targets are being scraped
- **Grafana Performance** - Check dashboard load times
- **Loki Ingestion** - Verify log ingestion rates
- **AlertManager Status** - Ensure alerts are being delivered

## Configuration

### Adding New Metrics

1. **Application Code** - Add Micrometer metrics
2. **Prometheus Config** - Add scrape target if needed
3. **Grafana Dashboard** - Create visualization
4. **Alert Rules** - Add alerts for new metrics

### Adding New Services

1. **Service Configuration** - Add Prometheus endpoint
2. **Prometheus Scraping** - Add to `prometheus.yml`
3. **Logging** - Ensure structured logging
4. **Tracing** - Add Spring Sleuth dependency
5. **Dashboards** - Update Grafana dashboards

### Scaling Monitoring

For production environments:
- **Prometheus HA** - Deploy multiple Prometheus instances
- **Grafana HA** - Use external database for Grafana
- **Loki Scaling** - Use distributed Loki deployment
- **Long-term Storage** - Configure remote storage for metrics

## Security Considerations

- **Access Control** - Secure Grafana with proper authentication
- **Network Security** - Use internal networks for monitoring traffic
- **Data Retention** - Configure appropriate retention policies
- **Sensitive Data** - Avoid logging sensitive information

## Performance Tuning

### Prometheus Optimization
- **Scrape Intervals** - Balance frequency vs. resource usage
- **Retention** - Configure based on storage capacity
- **Recording Rules** - Pre-compute expensive queries

### Grafana Optimization
- **Query Optimization** - Use efficient PromQL queries
- **Dashboard Performance** - Limit time ranges and data points
- **Caching** - Enable query result caching

### Loki Optimization
- **Log Parsing** - Use efficient log parsing rules
- **Retention** - Configure log retention policies
- **Compression** - Enable log compression