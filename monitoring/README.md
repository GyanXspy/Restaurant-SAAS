# Monitoring and Observability

This directory contains the complete monitoring and observability stack for the Restaurant Food Ordering System.

## Components

### 1. Metrics Collection (Prometheus)
- **Prometheus**: Collects metrics from all microservices
- **Custom Metrics**: Business-specific metrics for sagas, events, and processing
- **Exporters**: Kafka, MySQL, MongoDB, and Node exporters for infrastructure metrics

### 2. Visualization (Grafana)
- **Dashboards**: Pre-configured dashboards for system overview
- **Panels**: Service health, saga processing, event handling, database performance
- **Alerting**: Visual alerts and notifications

### 3. Alerting (AlertManager)
- **Alert Rules**: Comprehensive alerting rules for system and business metrics
- **Notification Channels**: Email, Slack, and webhook integrations
- **Alert Routing**: Different severity levels with appropriate routing

### 4. Distributed Tracing (Zipkin)
- **Request Tracing**: End-to-end request tracing across microservices
- **Saga Tracing**: Detailed tracing of saga execution steps
- **Performance Analysis**: Identify bottlenecks and latency issues

## Quick Start

### 1. Start Monitoring Stack
```bash
cd monitoring
docker-compose -f docker-compose-monitoring.yml up -d
```

### 2. Access Dashboards
- **Grafana**: http://localhost:3000 (admin/admin)
- **Prometheus**: http://localhost:9090
- **AlertManager**: http://localhost:9093
- **Zipkin**: http://localhost:9411

### 3. Configure Services
Add the monitoring profile to your service configurations:
```yaml
spring:
  profiles:
    include: monitoring
```

## Metrics Overview

### Application Metrics

#### Saga Processing
- `sagas_started_total`: Total number of sagas started
- `sagas_completed_total`: Total number of sagas completed successfully
- `sagas_failed_total`: Total number of sagas that failed
- `sagas_active_current`: Current number of active sagas
- `sagas_execution_duration`: Time taken to execute sagas

#### Event Processing
- `events_published_total`: Total number of events published
- `events_consumed_total`: Total number of events consumed
- `events_processing_errors_total`: Total number of event processing errors
- `events_processing_duration`: Time taken to process events

#### HTTP Requests
- `http_server_requests_total`: Total HTTP requests
- `http_server_requests_duration`: HTTP request duration

### Infrastructure Metrics

#### Database (MySQL/MongoDB)
- Connection pool utilization
- Query execution time
- Active connections
- Database response time

#### Kafka
- Consumer lag
- Message throughput
- Partition status
- Broker availability

#### System Resources
- CPU usage
- Memory usage
- Disk space
- Network I/O

## Alert Rules

### Critical Alerts
- **ServiceDown**: Service is unavailable
- **KafkaPartitionOffline**: Kafka partition is offline
- **DiskSpaceLow**: Disk space below 10%

### Warning Alerts
- **HighErrorRate**: HTTP error rate > 5%
- **LowSagaSuccessRate**: Saga success rate < 95%
- **HighActiveSagas**: Active sagas > 100
- **DatabaseConnectionPoolHigh**: Connection pool > 80%
- **EventProcessingLag**: Event processing time > 5s

### Business Alerts
- **OrderProcessingStalled**: Orders received but no sagas started
- **PaymentFailureSpike**: High payment failure rate
- **CartValidationFailureSpike**: High cart validation failure rate

## Dashboard Panels

### System Overview
1. **Service Health Status**: Real-time service availability
2. **Request Rate**: HTTP request throughput per service
3. **Response Time Percentiles**: 50th, 95th, 99th percentiles
4. **Error Rate**: HTTP error rates by service

### Saga Processing
1. **Saga Processing Metrics**: Started, completed, failed sagas
2. **Active Sagas**: Current number of active sagas
3. **Saga Execution Time**: Processing duration distribution
4. **Compensation Events**: Saga rollback metrics

### Event Processing
1. **Event Processing Rate**: Published vs consumed events
2. **Event Processing Errors**: Error rates by event type
3. **Event Processing Duration**: Processing time metrics
4. **Kafka Consumer Lag**: Message processing lag

### Infrastructure
1. **Database Connection Pool**: Active vs max connections
2. **JVM Memory Usage**: Heap memory utilization
3. **System Resources**: CPU, memory, disk usage
4. **Network I/O**: Network traffic metrics

## Configuration

### Service Configuration
Each service includes monitoring configuration in `application-monitoring.yml`:
- Health check endpoints
- Metrics exposure
- Tracing configuration
- Custom metric tags

### Prometheus Configuration
- Scrape intervals and timeouts
- Service discovery
- Alert rule evaluation
- Retention policies

### Grafana Configuration
- Data source configuration
- Dashboard provisioning
- Alert notification channels
- User management

## Troubleshooting

### Common Issues

1. **Metrics Not Appearing**
   - Check service health endpoints: `/actuator/health`
   - Verify Prometheus targets: http://localhost:9090/targets
   - Check service logs for metric registration errors

2. **Alerts Not Firing**
   - Verify alert rules syntax in Prometheus
   - Check AlertManager configuration
   - Test notification channels

3. **Tracing Not Working**
   - Verify Zipkin endpoint configuration
   - Check trace sampling configuration
   - Ensure trace context propagation

### Debug Commands

```bash
# Check Prometheus targets
curl http://localhost:9090/api/v1/targets

# Check AlertManager status
curl http://localhost:9093/api/v1/status

# Check service metrics
curl http://localhost:8080/actuator/prometheus

# Check service health
curl http://localhost:8080/actuator/health
```

## Production Considerations

### Security
- Enable authentication for Grafana
- Secure Prometheus and AlertManager endpoints
- Use HTTPS for external access
- Implement proper RBAC

### Scalability
- Configure Prometheus federation for multiple clusters
- Use remote storage for long-term retention
- Implement metric cardinality limits
- Set up horizontal scaling for Grafana

### Reliability
- Set up Prometheus HA with multiple replicas
- Configure AlertManager clustering
- Implement backup strategies for dashboards
- Monitor the monitoring stack itself

### Performance
- Optimize scrape intervals based on requirements
- Configure appropriate retention policies
- Use recording rules for expensive queries
- Implement metric filtering and aggregation