# Event-Driven User Creation Flow

## Architecture:
```
Client → REST API → Kafka → Consumer → Database
```

## Step-by-Step Flow:

### 1. **Client sends POST request**
```bash
POST http://localhost:8081/api/users
Content-Type: application/json

{
  "userId": "user123",
  "email": "user123@example.com",
  "profile": {
    "firstName": "John",
    "lastName": "Doe",
    "phone": "+1234567890"
  }
}
```

### 2. **Controller receives request**
- `UserController.createUser()` is called
- Returns HTTP 202 (Accepted) immediately
- User is NOT saved to database yet

### 3. **Service publishes to Kafka**
- `UserService.publishUserCreationEvent()` is called
- Creates JSON event:
```json
{
  "userId": "user123",
  "email": "user123@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+1234567890"
}
```
- Publishes to Kafka topic: `user-creation-events`

### 4. **Kafka stores the event**
- Event is persisted in Kafka
- Available for consumption

### 5. **Consumer receives event**
- `UserEventConsumer.consumeUserCreationEvent()` is triggered
- Parses the JSON message
- Validates required fields

### 6. **Service saves to database**
- `UserService.saveUserFromEvent()` is called
- Creates User entity
- Saves to MongoDB
- User is now in database!

## Benefits:

✅ **Asynchronous Processing** - API responds immediately
✅ **Decoupling** - API and database operations are independent
✅ **Reliability** - Kafka ensures message delivery
✅ **Scalability** - Can add multiple consumers
✅ **Audit Trail** - All events are logged in Kafka
✅ **Retry Logic** - Failed messages can be retried

## Testing:

### 1. Start Kafka:
```bash
# Start Zookeeper
bin\windows\zookeeper-server-start.bat config\zookeeper.properties

# Start Kafka
bin\windows\kafka-server-start.bat config\server.properties
```

### 2. Start MongoDB:
```bash
mongod
```

### 3. Start User Service:
```bash
cd Restaurant-SAAS/user-service
mvn spring-boot:run
```

### 4. Create a user:
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "user123",
    "email": "user123@example.com",
    "profile": {
      "firstName": "John",
      "lastName": "Doe",
      "phone": "+1234567890"
    }
  }'
```

### 5. Check Kafka topic:
```bash
bin\windows\kafka-console-consumer.bat --topic user-creation-events --from-beginning --bootstrap-server localhost:9092
```

### 6. Check MongoDB:
```bash
mongosh user_service
db.users.find().pretty()
```

## Flow Diagram:

```
┌─────────┐     POST      ┌────────────┐
│ Client  │ ────────────> │ Controller │
└─────────┘               └────────────┘
                                 │
                                 ▼
                          ┌────────────┐
                          │  Service   │
                          │ (Publish)  │
                          └────────────┘
                                 │
                                 ▼
                          ┌────────────┐
                          │   Kafka    │
                          │   Topic    │
                          └────────────┘
                                 │
                                 ▼
                          ┌────────────┐
                          │  Consumer  │
                          └────────────┘
                                 │
                                 ▼
                          ┌────────────┐
                          │  Service   │
                          │   (Save)   │
                          └────────────┘
                                 │
                                 ▼
                          ┌────────────┐
                          │  MongoDB   │
                          └────────────┘
```

## Error Handling:

- Invalid JSON messages are logged and skipped
- Missing required fields are logged and skipped
- Duplicate users are logged and skipped
- All errors are logged for debugging
