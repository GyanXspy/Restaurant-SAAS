# DLQ Replay System - Complete Guide

## 🎯 Overview

Production-ready Dead Letter Queue (DLQ) replay mechanism for failed Kafka events. Backend-only implementation with REST API ready for future UI integration.

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    DLQ REPLAY FLOW                               │
└─────────────────────────────────────────────────────────────────┘

Event Publish Fails (after 3 retries)
    ↓
Send to DLQ Topic: order-events-dlq
    ↓
DlqConsumer listens to DLQ
    ↓
Store in dlq_replay_records table (MySQL)
    ↓
Status: PENDING
    ↓
Manual Replay via REST API
    ↓
POST /api/dlq/replay/{eventId}
    ↓
Republish to Kafka
    ↓
Status: REPLAYED or FAILED
```

## 📊 Database Schema

### dlq_replay_records Table

```sql
CREATE TABLE dlq_replay_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    original_event TEXT,
    failure_reason TEXT,
    failure_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    replay_attempt_time TIMESTAMP NOT NULL,
    replay_result TEXT,
    replay_attempts INT,
    
    INDEX idx_event_id (event_id),
    INDEX idx_status (status),
    INDEX idx_aggregate_id (aggregate_id)
);
```

## 🔄 DLQ Replay Status

| Status | Description |
|--------|-------------|
| **PENDING** | Event in DLQ, waiting for replay |
| **REPLAYED** | Successfully replayed to Kafka |
| **FAILED** | Replay attempt failed |
| **SKIPPED** | Already replayed, skipped duplicate |

## 🚀 REST API Endpoints

### 1. Replay Single Event
```bash
POST /api/dlq/replay/{eventId}

# Example
curl -X POST http://localhost:8084/api/dlq/replay/abc-123-def-456

# Response
{
  "eventId": "abc-123-def-456",
  "success": true,
  "message": "Successfully replayed",
  "status": "REPLAYED"
}
```

### 2. Get DLQ Statistics
```bash
GET /api/dlq/stats

# Response
{
  "pendingCount": 5,
  "replayedCount": 10,
  "failedCount": 2,
  "skippedCount": 1,
  "totalCount": 18
}
```

### 3. Get Pending Messages
```bash
GET /api/dlq/pending

# Response
[
  {
    "id": 1,
    "eventId": "abc-123",
    "aggregateId": "order-456",
    "eventType": "OrderCreated",
    "failureReason": "Connection timeout",
    "failureTime": "2025-12-25T10:00:00",
    "status": "PENDING",
    "replayAttempts": 0
  }
]
```

### 4. Get Messages by Status
```bash
GET /api/dlq/status/{status}

# Examples
GET /api/dlq/status/PENDING
GET /api/dlq/status/REPLAYED
GET /api/dlq/status/FAILED
```

### 5. Get Event Details
```bash
GET /api/dlq/event/{eventId}

# Response
{
  "id": 1,
  "eventId": "abc-123",
  "aggregateId": "order-456",
  "eventType": "OrderCreated",
  "originalEvent": "{...}",
  "failureReason": "Connection timeout",
  "failureTime": "2025-12-25T10:00:00",
  "status": "PENDING",
  "replayAttemptTime": "2025-12-25T10:05:00",
  "replayResult": null,
  "replayAttempts": 0
}
```

## 🔐 Key Features

### 1. Idempotency
- Won't replay events already successfully replayed
- Checks `eventId` before replay
- Returns `SKIPPED` status for duplicates

### 2. Audit Trail
- Every replay attempt logged in database
- Tracks: attempts, timestamps, results
- Complete history for compliance

### 3. Safe Replay
- Validates event before republishing
- Deserializes and validates structure
- Catches errors gracefully

### 4. No Automatic Retries from DLQ
- Manual replay only via REST API
- Prevents infinite retry loops
- Human oversight required

### 5. Long Retention
- DLQ retention: 7 days (configurable)
- Database records: permanent
- Sufficient time for investigation

## 📈 Observability

### Logging
```java
// DLQ message received
log.info("Received DLQ message: {}", message);

// Replay attempt
log.info("Attempting to replay event: {}", eventId);

// Replay success
log.info("Successfully replayed event: {}", eventId);

// Replay failure
log.error("Failed to replay event: {}", eventId, exception);
```

### Metrics (Future)
- DLQ message count
- Replay success rate
- Replay latency
- Failed replay count

## 🧪 Testing

### 1. Simulate Event Failure
```bash
# Stop Kafka temporarily
docker-compose stop kafka

# Create order (will fail and go to DLQ)
POST http://localhost:8084/api/orders
{
  "customerId": "customer-123",
  "restaurantId": "restaurant-456",
  "totalAmount": 45.99,
  "items": [...]
}

# Start Kafka
docker-compose start kafka
```

### 2. Check DLQ Messages
```bash
# Get pending messages
curl http://localhost:8084/api/dlq/pending

# Get stats
curl http://localhost:8084/api/dlq/stats
```

### 3. Replay Event
```bash
# Replay by eventId
curl -X POST http://localhost:8084/api/dlq/replay/{eventId}

# Verify status changed to REPLAYED
curl http://localhost:8084/api/dlq/event/{eventId}
```

## 🔧 Configuration

### application.properties
```properties
# DLQ Topic
kafka.topic.dead-letter=order-events-dlq

# DLQ Retention (7 days)
spring.kafka.admin.properties.retention.ms=604800000

# DLQ Consumer Group
# Automatically set to: ${spring.application.name}-dlq-consumer
```

## 📦 Components

### 1. DlqConsumer
- Listens to DLQ topic
- Stores messages in database
- Logs all DLQ events

### 2. DlqReplayService
- Core replay logic
- Idempotency checks
- Event deserialization
- Kafka republishing

### 3. DlqReplayController
- REST API endpoints
- Future UI integration point
- Statistics and monitoring

### 4. DlqReplayRepository
- JPA repository
- Query by status, eventId, aggregateId
- Audit trail persistence

## 🎯 Use Cases

### 1. Kafka Downtime
```
Scenario: Kafka was down during order creation
Action: Replay failed OrderCreatedEvent
Result: Event successfully published to Kafka
```

### 2. Network Issues
```
Scenario: Network timeout during event publishing
Action: Investigate and replay after network recovery
Result: Event reaches downstream services
```

### 3. Configuration Errors
```
Scenario: Wrong Kafka broker configuration
Action: Fix config, replay all pending events
Result: All events successfully published
```

## 🚀 Future Enhancements

### Phase 2 (Future)
- [ ] Frontend UI with replay button
- [ ] Bulk replay (replay all pending)
- [ ] Scheduled replay (retry at specific times)
- [ ] DLQ dashboard with charts
- [ ] Email alerts for DLQ messages
- [ ] Automatic replay with backoff

### Phase 3 (Future)
- [ ] Event filtering and search
- [ ] Replay history timeline
- [ ] Export DLQ messages to CSV
- [ ] Replay simulation (dry-run)
- [ ] Custom replay strategies

## 📊 Monitoring Queries

### Check DLQ Size
```sql
SELECT status, COUNT(*) as count
FROM dlq_replay_records
GROUP BY status;
```

### Recent Failures
```sql
SELECT event_id, event_type, failure_reason, failure_time
FROM dlq_replay_records
WHERE status = 'PENDING'
ORDER BY failure_time DESC
LIMIT 10;
```

### Replay Success Rate
```sql
SELECT 
    COUNT(CASE WHEN status = 'REPLAYED' THEN 1 END) as replayed,
    COUNT(CASE WHEN status = 'FAILED' THEN 1 END) as failed,
    COUNT(*) as total
FROM dlq_replay_records
WHERE replay_attempts > 0;
```

## 🔒 Security Considerations

1. **Authentication**: Add authentication to replay endpoints (future)
2. **Authorization**: Only admins should replay events
3. **Rate Limiting**: Prevent replay abuse
4. **Audit Logging**: Log who triggered replays

## 📝 Best Practices

1. **Investigate Before Replay**: Understand why event failed
2. **Fix Root Cause**: Don't just replay, fix the issue
3. **Monitor DLQ Size**: Alert if DLQ grows too large
4. **Regular Cleanup**: Archive old replayed events
5. **Test Replays**: Verify event structure before replay

## 🎉 Summary

✅ **Backend-only DLQ replay system**
✅ **REST API ready for future UI**
✅ **Idempotent and safe replay**
✅ **Complete audit trail**
✅ **No automatic retries from DLQ**
✅ **Long retention (7 days)**
✅ **Production-ready observability**

**Ready for production use and future UI integration!** 🚀
