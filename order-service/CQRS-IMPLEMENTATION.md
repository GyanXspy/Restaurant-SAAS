# CQRS Pattern with Polyglot Persistence - Order Service

## Overview
The Order Service implements **full CQRS (Command Query Responsibility Segregation)** with **polyglot persistence** - using **MySQL for writes** and **MongoDB for reads**.

## Architecture

### Polyglot Persistence CQRS

**Write Database (MySQL - order_write_db)**
- Relational database for ACID transactions
- Stores the source of truth
- Handles all commands (state changes)
- Normalized schema for data integrity

**Read Database (MongoDB - order_read_db)**
- Document database for flexible queries
- Optimized for fast reads
- Denormalized documents
- Indexed for common query patterns
- Updated via projection service

### Why Polyglot Persistence?

1. **MySQL for Writes** - ACID transactions, referential integrity, strong consistency
2. **MongoDB for Reads** - Fast queries, flexible schema, horizontal scaling, denormalized data

## Database Schema

### Write Database (MySQL - order_write_db)
```sql
-- Normalized relational schema
orders (
    id VARCHAR(36) PRIMARY KEY,
    customer_id VARCHAR(255) NOT NULL,
    restaurant_id VARCHAR(255) NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    payment_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
)

order_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(36) NOT NULL,
    menu_item_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id)
)
```

### Read Database (MongoDB - order_read_db)
```javascript
// Denormalized document schema
{
    _id: "order-uuid",
    customerId: "customer-123",      // indexed
    restaurantId: "restaurant-456",  // indexed
    totalAmount: 45.99,
    status: "CONFIRMED",             // indexed
    paymentId: "payment-789",
    createdAt: ISODate("2025-01-01"), // indexed
    updatedAt: ISODate("2025-01-01"),
    itemCount: 2,                    // denormalized
    itemsSummary: "Pizza x2, Salad x1" // denormalized
}
```

## Configuration

### application.properties
```properties
# Write Database (MySQL)
spring.datasource.write.jdbc-url=jdbc:mysql://localhost:3306/order_write_db
spring.datasource.write.username=root
spring.datasource.write.password=Satya@123

# Read Database (MongoDB)
spring.data.mongodb.uri=mongodb://localhost:27017/order_read_db
```

## Data Flow

```
Command → CommandHandler → MySQL (Write) → ProjectionService → MongoDB (Read)
                                        ↓
                                     Events (Kafka)

Query → QueryHandler → MongoDB (Read) → Response
```

## Setup Instructions

1. Create MySQL database:
```sql
CREATE DATABASE order_write_db;
```

2. MongoDB database will be auto-created on first write

3. Start the service - tables/collections will be auto-created

4. Write operations go to MySQL `order_write_db`

5. Read operations go to MongoDB `order_read_db`

6. Projection service keeps them synchronized

## Benefits

1. **Best of Both Worlds** - ACID for writes, flexibility for reads
2. **Scalability** - MongoDB can scale horizontally for reads
3. **Performance** - Optimized queries with denormalized MongoDB documents
4. **Flexibility** - Easy to add new query patterns in MongoDB
5. **Resilience** - MongoDB failure doesn't affect writes
6. **Technology Fit** - Each database optimized for its workload

## API Endpoints

### Commands (Write to MySQL)
```
POST   /api/orders                    - Create order
PUT    /api/orders/{id}/confirm       - Confirm order
PUT    /api/orders/{id}/cancel        - Cancel order
```

### Queries (Read from MongoDB)
```
GET    /api/orders/{id}               - Get order by ID
GET    /api/orders/customer/{id}      - Get customer orders
```

## Example Usage

### Create Order (Command → MySQL)
```bash
POST http://localhost:8084/api/orders
Content-Type: application/json

{
    "customerId": "customer-123",
    "restaurantId": "restaurant-456",
    "totalAmount": 45.99,
    "items": [
        {
            "menuItemId": "item-1",
            "name": "Margherita Pizza",
            "price": 12.99,
            "quantity": 2
        }
    ]
}

Response: "order-uuid-here" (201 Created)
```

### Get Order (Query ← MongoDB)
```bash
GET http://localhost:8084/api/orders/{orderId}

Response: OrderResponse JSON (200 OK)
```

## Future Enhancements

- [ ] Integrate Kafka event publishing in CommandHandler
- [ ] Add async projection via Kafka events for eventual consistency
- [ ] Implement read model rebuild from MySQL
- [ ] Add MongoDB aggregation pipelines for complex queries
- [ ] Implement caching layer (Redis) for hot data
- [ ] Add MongoDB sharding for massive scale
- [ ] Implement saga orchestration for distributed transactions
