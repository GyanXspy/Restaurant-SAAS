# DLQ Testing - Troubleshooting Guide

## ✅ What Was Fixed

1. **Added @EnableRetry** - Spring Retry now enabled for @Retryable annotations
2. **Enhanced Logging** - DEBUG level for Kafka and event publishing
3. **Added Kafka Consumer Factory** - DLQ consumer properly configured
4. **Enhanced DLQ Consumer Logs** - More detailed logging for debugging

## 🧪 Step-by-Step Testing (UPDATED)

### Prerequisites

```bash
# Ensure these are running
docker ps | grep mysql
docker ps | grep mongodb
docker ps | grep kafka
docker ps | grep zookeeper
```

### Step 1: Start Application with Kafka Running

```bash
# Make sure Kafka is UP
docker-compose up -d kafka zookeeper

# Start application
mvn spring-boot:run
```

**Watch for these logs on startup**:
```
Started OrderServiceApplication
DLQ CONSUMER: Kafka listener initialized
```

### Step 2: Stop Kafka (Simulate Failure)

```bash
# Stop Kafka
docker-compose stop kafka

# Verify it's stopped
docker ps | grep kafka  # Should show nothing
```

### Step 3: Create Order (Will Fail to Publish)

```bash
POST http://localhost:8084/api/orders
Content-Type: application/json

{
  "customerId": "customer-TEST-123",
  "restaurantId": "restaurant-TEST-456",
  "totalAmount": 99.99,
  "items": [
    {
      "menuItemId": "item-TEST-1",
      "name": "Test Pizza",
      "price": 29.99,
      "quantity": 2
    },
    {
      "menuItemId": "item-TEST-2",
      "name": "Test Drink",
      "price": 9.99,
      "quantity": 4
    }
  ]
}
```

**Expected Response**: `201 Created` with order ID

**Watch Application Logs** (CRITICAL):
```
Order created with ID: <orderId>
Publishing event <eventId> to topic order-events
Failed to publish event <eventId>
Retry attempt 1 of 3
Retry attempt 2 of 3
Retry attempt 3 of 3
Sending failed event <eventId> to dead letter queue
Sent failed event <eventId> to dead letter queue
```

### Step 4: Verify Event in Event Store

```sql
-- Check MySQL event_store table
SELECT * FROM event_store ORDER BY created_at DESC LIMIT 1;

-- You should see the OrderCreatedEvent
```

### Step 5: Start Kafka

```bash
# Start Kafka
docker-compose start kafka

# Wait 30 seconds for Kafka to fully start
sleep 30
```

**Watch Application Logs**:
```
=== DLQ CONSUMER: Received message ===
DLQ Message: {"originalEvent":{...},"failureReason":"...","failureTime":"..."}
Parsed - Failure Reason: Failed to publish event: <eventId>
Event Details - ID: <eventId>, Type: OrderCreated
=== DLQ CONSUMER: Successfully stored message for event: <eventId> ===
```

### Step 6: Check DLQ Stats

```bash
GET http://localhost:8084/api/dlq/stats
```

**Expected Response**:
```json
{
  "pendingCount": 1,
  "replayedCount": 0,
  "failedCount": 0,
  "skippedCount": 0,
  "totalCount": 1
}
```

### Step 7: Get Pending Messages

```bash
GET http://localhost:8084/api/dlq/pending
```

**Expected Response**:
```json
[
  {
    "id": 1,
    "eventId": "abc-123-def-456",
    "aggregateId": "order-789",
    "eventType": "OrderCreated",
    "status": "PENDING",
    "replayAttempts": 0
  }
]
```

### Step 8: Replay Event

```bash
POST http://localhost:8084/api/dlq/replay/{eventId}
```

**Expected Response**:
```json
{
  "eventId": "abc-123-def-456",
  "success": true,
  "message": "Successfully replayed",
  "status": "REPLAYED"
}
```

### Step 9: Verify Replay

```bash
GET http://localhost:8084/api/dlq/stats
```

**Expected Response**:
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

## 🔍 Troubleshooting: If DLQ Still Shows 0

### Issue 1: Event Published Successfully (Kafka Was Running)

**Check**:
```bash
# Verify Kafka is actually stopped
docker ps | grep kafka  # Should show nothing

# Check application logs
grep "Successfully published event" logs/order-service.log
```

**If you see "Successfully published"**: Kafka was running, event didn't fail

**Solution**: Make sure Kafka is completely stopped before creating order

### Issue 2: Retry Not Working

**Check Application Logs**:
```bash
grep "Retry attempt" logs/order-service.log
```

**If no retry logs**: Spring Retry not enabled

**Solution**: Verify `@EnableRetry` is in OrderServiceApplication.java

### Issue 3: DLQ Consumer Not Running

**Check Application Logs**:
```bash
grep "DLQ CONSUMER" logs/order-service.log
```

**If no DLQ consumer logs**: Consumer not initialized

**Check**:
1. Kafka listener container factory configured
2. DlqConsumer class has `@Component` annotation
3. Application has `@EnableKafka` annotation

### Issue 4: Event Not Going to DLQ

**Check Application Logs**:
```bash
grep "Sending failed event" logs/order-service.log
grep "Sent failed event" logs/order-service.log
```

**If no DLQ send logs**: Event not being sent to DLQ

**Possible Causes**:
1. Exception not being caught properly
2. DLQ topic name mismatch
3. KafkaTemplate not configured

### Issue 5: DLQ Consumer Not Picking Up Messages

**Check Kafka Topic**:
```bash
# List topics
kafka-topics --bootstrap-server localhost:9092 --list

# Check if order-events-dlq exists
kafka-topics --bootstrap-server localhost:9092 --describe --topic order-events-dlq

# Consume from DLQ manually
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic order-events-dlq \
  --from-beginning
```

**If messages in topic but not consumed**: Consumer group issue

**Solution**:
```bash
# Reset consumer group offset
kafka-consumer-groups --bootstrap-server localhost:9092 \
  --group order-service-dlq-consumer \
  --reset-offsets --to-earliest \
  --topic order-events-dlq \
  --execute
```

---

## 📊 Database Verification

### Check Event Store
```sql
-- Check if event was stored
SELECT 
    event_id, 
    aggregate_id, 
    event_type, 
    version, 
    occurred_on 
FROM event_store 
ORDER BY created_at DESC 
LIMIT 5;
```

### Check DLQ Replay Records
```sql
-- Check if DLQ message was stored
SELECT 
    event_id, 
    aggregate_id, 
    event_type, 
    status, 
    failure_reason,
    failure_time,
    replay_attempts
FROM dlq_replay_records 
ORDER BY failure_time DESC;
```

### Check Orders Table
```sql
-- Verify order was created
SELECT 
    id, 
    customer_id, 
    restaurant_id, 
    status, 
    total_amount 
FROM orders 
ORDER BY created_at DESC 
LIMIT 5;
```

---

## 🎯 Complete Test Script

```bash
#!/bin/bash

echo "=== DLQ Test Script ==="

# 1. Stop Kafka
echo "Step 1: Stopping Kafka..."
docker-compose stop kafka
sleep 5

# 2. Create Order
echo "Step 2: Creating order (will fail to publish)..."
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8084/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-TEST-123",
    "restaurantId": "restaurant-TEST-456",
    "totalAmount": 99.99,
    "items": [
      {
        "menuItemId": "item-TEST-1",
        "name": "Test Pizza",
        "price": 29.99,
        "quantity": 2
      }
    ]
  }')

echo "Order Response: $ORDER_RESPONSE"

# 3. Wait for retries
echo "Step 3: Waiting for retry attempts (10 seconds)..."
sleep 10

# 4. Start Kafka
echo "Step 4: Starting Kafka..."
docker-compose start kafka
sleep 30

# 5. Check DLQ Stats
echo "Step 5: Checking DLQ stats..."
curl -s http://localhost:8084/api/dlq/stats | jq

# 6. Get Pending Messages
echo "Step 6: Getting pending messages..."
PENDING=$(curl -s http://localhost:8084/api/dlq/pending | jq)
echo "$PENDING"

# 7. Extract eventId and replay
EVENT_ID=$(echo "$PENDING" | jq -r '.[0].eventId')
if [ "$EVENT_ID" != "null" ]; then
    echo "Step 7: Replaying event: $EVENT_ID"
    curl -s -X POST http://localhost:8084/api/dlq/replay/$EVENT_ID | jq
    
    # 8. Verify replay
    echo "Step 8: Verifying replay..."
    sleep 2
    curl -s http://localhost:8084/api/dlq/stats | jq
else
    echo "ERROR: No pending messages found!"
fi

echo "=== Test Complete ==="
```

---

## ✅ Success Indicators

You'll know it's working when you see:

1. ✅ **Order Created**: `201 Created` response
2. ✅ **Publish Failed**: `Failed to publish event` in logs
3. ✅ **Retries**: `Retry attempt 1 of 3`, `2 of 3`, `3 of 3` in logs
4. ✅ **DLQ Send**: `Sent failed event to dead letter queue` in logs
5. ✅ **DLQ Receive**: `=== DLQ CONSUMER: Received message ===` in logs
6. ✅ **DLQ Stored**: `Successfully stored message for event` in logs
7. ✅ **Stats Show 1**: `pendingCount: 1` in API response
8. ✅ **Replay Success**: `status: "REPLAYED"` in API response
9. ✅ **Stats Updated**: `replayedCount: 1, pendingCount: 0`

---

## 🚨 Common Mistakes

1. **Kafka not fully stopped** - Check with `docker ps`
2. **Not waiting long enough** - Wait 30 seconds after starting Kafka
3. **Wrong topic name** - Verify `order-events-dlq` in properties
4. **Consumer group conflict** - Reset consumer group offsets
5. **Logs not checked** - Always check application logs first

---

## 📝 Quick Checklist

Before testing:
- [ ] MySQL running
- [ ] MongoDB running
- [ ] Zookeeper running
- [ ] Application started successfully
- [ ] Kafka STOPPED for test
- [ ] Logs visible (tail -f logs/order-service.log)

After creating order:
- [ ] Order created (201 response)
- [ ] Logs show "Failed to publish"
- [ ] Logs show retry attempts
- [ ] Logs show "Sent to DLQ"

After starting Kafka:
- [ ] Wait 30 seconds
- [ ] Logs show "DLQ CONSUMER: Received"
- [ ] Logs show "Successfully stored"
- [ ] API shows pendingCount: 1

After replay:
- [ ] Replay returns success: true
- [ ] API shows replayedCount: 1
- [ ] API shows pendingCount: 0

---

**The DLQ system is now fully configured and ready for testing!** 🎉
