# DLQ Testing Guide

## 🎯 Quick Test (5 Minutes)

### Prerequisites
```bash
# Ensure services are running
docker ps | grep -E "mysql|mongodb|kafka|zookeeper"
```

### Step-by-Step Test

#### 1. Stop Kafka
```bash
docker-compose stop kafka
```

#### 2. Trigger Test Event
```bash
POST http://localhost:8084/api/test/publish-event
```

**Expected Response**:
```json
{
  "success": false,
  "message": "Failed to publish: ...",
  "error": "EventPublishingException"
}
```

**Watch Application Logs**:
```
=== TEST: Publishing event directly ===
Publishing test event: <eventId>
Failed to publish event
Retry attempt 1 of 3
Retry attempt 2 of 3
Retry attempt 3 of 3
Sending failed event to dead letter queue
Sent failed event to dead letter queue
```

#### 3. Start Kafka
```bash
docker-compose start kafka
# Wait 30 seconds for Kafka to start
```

**Watch Application Logs**:
```
=== DLQ CONSUMER: Received message ===
DLQ Message: {...}
Event Details - ID: <eventId>, Type: OrderCreated
=== DLQ CONSUMER: Successfully stored message ===
```

#### 4. Check DLQ Stats
```bash
GET http://localhost:8084/api/dlq/stats
```

**Expected**:
```json
{
  "pendingCount": 1,
  "replayedCount": 0,
  "failedCount": 0,
  "skippedCount": 0,
  "totalCount": 1
}
```

#### 5. Get Pending Messages
```bash
GET http://localhost:8084/api/dlq/pending
```

**Copy the `eventId` from response**

#### 6. Replay Event
```bash
POST http://localhost:8084/api/dlq/replay/{eventId}
```

**Expected**:
```json
{
  "eventId": "abc-123-...",
  "success": true,
  "message": "Successfully replayed",
  "status": "REPLAYED"
}
```

#### 7. Verify Success
```bash
GET http://localhost:8084/api/dlq/stats
```

**Expected**:
```json
{
  "pendingCount": 0,
  "replayedCount": 1,
  "failedCount": 0,
  "skippedCount": 0,
  "totalCount": 1
}
```

---

## 🔧 Reset Failed Event

If replay fails, reset and try again:

```bash
# 1. Reset to PENDING
PUT http://localhost:8084/api/dlq/reset/{eventId}

# 2. Replay again
POST http://localhost:8084/api/dlq/replay/{eventId}
```

---

## 📊 All DLQ Endpoints

### Get Statistics
```bash
GET http://localhost:8084/api/dlq/stats
```

### Get Pending Messages
```bash
GET http://localhost:8084/api/dlq/pending
```

### Get Event Details
```bash
GET http://localhost:8084/api/dlq/event/{eventId}
```

### Get by Status
```bash
GET http://localhost:8084/api/dlq/status/PENDING
GET http://localhost:8084/api/dlq/status/REPLAYED
GET http://localhost:8084/api/dlq/status/FAILED
```

### Replay Event
```bash
POST http://localhost:8084/api/dlq/replay/{eventId}
```

### Reset Failed Event
```bash
PUT http://localhost:8084/api/dlq/reset/{eventId}
```

---

## 🧪 Test with Real Order

### 1. Stop Kafka
```bash
docker-compose stop kafka
```

### 2. Create Order
```bash
POST http://localhost:8084/api/orders
Content-Type: application/json

{
  "customerId": "customer-123",
  "restaurantId": "restaurant-456",
  "totalAmount": 99.99,
  "items": [
    {
      "menuItemId": "item-1",
      "name": "Pizza",
      "price": 29.99,
      "quantity": 2
    }
  ]
}
```

**Note**: Order will be created in database, but Kafka publish will fail and go to DLQ.

### 3. Follow steps 3-7 from Quick Test above

---

## 🐛 Troubleshooting

### Issue: DLQ Shows 0 Messages

**Check**:
```bash
# 1. Verify Kafka is stopped
docker ps | grep kafka  # Should show nothing

# 2. Check application logs
grep "Sent failed event" logs/order-service.log

# 3. Check DLQ consumer is running
grep "DLQ CONSUMER" logs/order-service.log
```

**Solution**: Use test endpoint which bypasses database transactions:
```bash
POST http://localhost:8084/api/test/publish-event
```

### Issue: Replay Fails

**Check**:
```bash
# 1. Get event details
GET http://localhost:8084/api/dlq/event/{eventId}

# 2. Check status
# If status is FAILED, reset it:
PUT http://localhost:8084/api/dlq/reset/{eventId}

# 3. Try replay again
POST http://localhost:8084/api/dlq/replay/{eventId}
```

### Issue: Event Not Found

**Check Database**:
```sql
SELECT * FROM dlq_replay_records 
ORDER BY failure_time DESC 
LIMIT 10;
```

---

## 📝 Test Script

```bash
#!/bin/bash

echo "=== DLQ Test Script ==="

# 1. Stop Kafka
echo "1. Stopping Kafka..."
docker-compose stop kafka
sleep 5

# 2. Trigger event
echo "2. Triggering test event..."
curl -X POST http://localhost:8084/api/test/publish-event
sleep 10

# 3. Start Kafka
echo "3. Starting Kafka..."
docker-compose start kafka
sleep 30

# 4. Check stats
echo "4. Checking DLQ stats..."
curl http://localhost:8084/api/dlq/stats | jq

# 5. Get pending
echo "5. Getting pending messages..."
PENDING=$(curl -s http://localhost:8084/api/dlq/pending)
echo "$PENDING" | jq

# 6. Extract eventId and replay
EVENT_ID=$(echo "$PENDING" | jq -r '.[0].eventId')
if [ "$EVENT_ID" != "null" ]; then
    echo "6. Replaying event: $EVENT_ID"
    curl -X POST http://localhost:8084/api/dlq/replay/$EVENT_ID | jq
    
    # 7. Verify
    echo "7. Verifying replay..."
    sleep 2
    curl http://localhost:8084/api/dlq/stats | jq
else
    echo "ERROR: No pending messages found!"
fi

echo "=== Test Complete ==="
```

---

## ✅ Success Indicators

You'll know it's working when:

1. ✅ Test endpoint returns error (Kafka down)
2. ✅ Logs show "Retry attempt 1 of 3"
3. ✅ Logs show "Sent failed event to dead letter queue"
4. ✅ After Kafka starts, logs show "DLQ CONSUMER: Received message"
5. ✅ DLQ stats show `pendingCount: 1`
6. ✅ Replay returns `success: true`
7. ✅ DLQ stats show `replayedCount: 1, pendingCount: 0`

---

## 🎯 Quick Commands

```bash
# Stop Kafka
docker-compose stop kafka

# Test event
curl -X POST http://localhost:8084/api/test/publish-event

# Start Kafka
docker-compose start kafka && sleep 30

# Check DLQ
curl http://localhost:8084/api/dlq/stats

# Get pending
curl http://localhost:8084/api/dlq/pending | jq

# Replay (replace {eventId})
curl -X POST http://localhost:8084/api/dlq/replay/{eventId}

# Verify
curl http://localhost:8084/api/dlq/stats
```

---

**DLQ System is Production-Ready!** 🎉
