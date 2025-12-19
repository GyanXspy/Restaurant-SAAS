# Database Optimization Guide

## Overview

This document outlines the database optimizations implemented for the Restaurant Food Ordering System, including indexing strategies, connection pooling, read replica support, and query optimization techniques.

## MySQL Services (Order & Payment)

### 1. Indexing Strategy

#### Order Service Indexes
- **Event Store Indexes**: Optimized for event sourcing queries
  - `idx_order_events_aggregate_id`: Fast event retrieval by aggregate
  - `idx_order_events_aggregate_version`: Event replay optimization
  - `idx_order_events_type`: Event type filtering
  - `idx_order_events_created_at`: Temporal queries

- **Read Model Indexes**: Optimized for CQRS query patterns
  - `idx_order_read_customer_created`: Customer order history
  - `idx_order_read_restaurant_created`: Restaurant order management
  - `idx_order_read_status_created`: Status-based filtering with sorting
  - Composite indexes for multi-field queries

#### Payment Service Indexes
- **Event Store Indexes**: Similar pattern to order service
- **Payment Indexes**: Transaction and audit queries
  - Customer payment history
  - Transaction ID lookups
  - Payment method analytics
  - Status-based filtering

### 2. Connection Pooling (HikariCP)

#### Configuration Parameters
```yaml
hikari:
  maximum-pool-size: 20        # Max connections
  minimum-idle: 5              # Min idle connections
  idle-timeout: 300000         # 5 minutes
  max-lifetime: 1800000        # 30 minutes
  connection-timeout: 30000    # 30 seconds
  leak-detection-threshold: 60000  # 1 minute
```

#### MySQL-Specific Optimizations
- Prepared statement caching
- Batch statement rewriting
- Server-side prepared statements
- Connection validation queries

### 3. Read Replica Support

#### Architecture
- **Write Operations**: Routed to primary database
- **Read Operations**: Routed to read replicas
- **Automatic Routing**: Based on transaction type and method annotations

#### Implementation
- `@Transactional(readOnly = true)` → Read replica
- Query methods → Read replica
- Command methods → Primary database
- AspectJ interceptors for automatic routing

#### Configuration
```yaml
datasource:
  write:
    jdbc-url: jdbc:mysql://primary-host:3306/db
    maximum-pool-size: 20
  read:
    jdbc-url: jdbc:mysql://replica-host:3306/db
    maximum-pool-size: 15
    read-only: true
```

## MongoDB Services (User, Restaurant, Cart)

### 1. Indexing Strategy

#### User Service Indexes
- **Single Field Indexes**:
  - `userId` (unique)
  - `email` (unique)
  - `status`
  - `createdAt`, `updatedAt`

- **Compound Indexes**:
  - `{status: 1, createdAt: -1}`: Active users by date
  - `{profile.firstName: 1, profile.lastName: 1}`: Name searches
  - `{profile.addresses.city: 1}`: Location queries

#### Restaurant Service Indexes
- **Core Indexes**:
  - `restaurantId.value` (unique)
  - `{isActive: 1, cuisine: 1}`: Active restaurants by cuisine
  - `{address.city: 1, isActive: 1}`: Location-based queries

- **Menu Indexes**:
  - `{menu.itemId: 1}`: Menu item lookups
  - `{menu.category: 1, menu.available: 1}`: Category filtering
  - `{restaurantId.value: 1, menu.available: 1}`: Restaurant menu queries

#### Cart Service Indexes
- **Core Indexes**:
  - `cartId` (unique)
  - `{customerId: 1, restaurantId: 1}`: Customer-restaurant carts
  - `{customerId: 1, updatedAt: -1}`: Recent customer carts

- **TTL Index**:
  - `{expiresAt: 1}`: Automatic cart expiration

### 2. Connection Pooling

#### MongoDB Connection Settings
```yaml
mongodb:
  options:
    max-connection-pool-size: 20
    min-connection-pool-size: 5
    max-connection-idle-time: 300000
    connection-timeout: 30000
    socket-timeout: 30000
```

#### URI Parameters
- `maxPoolSize=20`: Maximum connections
- `minPoolSize=5`: Minimum connections
- `maxIdleTimeMS=300000`: Idle timeout
- `connectTimeoutMS=30000`: Connection timeout

### 3. Query Optimization

#### Projection Queries
- Return only required fields
- Reduce network overhead
- Improve query performance

#### Aggregation Pipelines
- Complex analytics queries
- Efficient data processing
- Reduced application logic

#### Example Optimized Queries
```javascript
// Efficient user search with projection
db.users.find(
  {"status": "ACTIVE"}, 
  {"userId": 1, "email": 1, "profile.firstName": 1}
)

// Restaurant search with compound index
db.restaurants.find({
  "cuisine": "Italian",
  "address.city": "New York",
  "isActive": true
})

// Cart cleanup with TTL
db.carts.createIndex({"expiresAt": 1}, {"expireAfterSeconds": 0})
```

## Performance Monitoring

### 1. Connection Pool Metrics
- Active connections
- Idle connections
- Connection wait time
- Pool exhaustion events

### 2. Query Performance
- Slow query logs
- Index usage statistics
- Query execution plans
- Cache hit ratios

### 3. Database Health Checks
- Connection availability
- Replication lag (MySQL)
- Index efficiency
- Storage utilization

## Best Practices

### 1. Index Management
- Regular index analysis
- Remove unused indexes
- Monitor index selectivity
- Update statistics regularly

### 2. Connection Pooling
- Size pools based on load
- Monitor connection leaks
- Configure timeouts appropriately
- Use connection validation

### 3. Query Optimization
- Use appropriate indexes
- Limit result sets
- Avoid N+1 queries
- Use batch operations

### 4. Read Replica Usage
- Route read-only queries
- Handle replication lag
- Monitor replica health
- Failover strategies

## Environment Configuration

### Development
- Smaller connection pools
- Relaxed timeouts
- Debug logging enabled
- Local databases

### Production
- Optimized pool sizes
- Strict timeouts
- Performance monitoring
- Read replicas enabled
- Connection encryption

## Troubleshooting

### Common Issues
1. **Connection Pool Exhaustion**
   - Increase pool size
   - Check for connection leaks
   - Optimize query performance

2. **Slow Queries**
   - Add appropriate indexes
   - Optimize query patterns
   - Use query profiling

3. **Replication Lag**
   - Monitor replica status
   - Check network connectivity
   - Optimize write patterns

4. **Index Inefficiency**
   - Analyze query patterns
   - Update index strategy
   - Remove redundant indexes

## Migration Strategy

### Index Creation
1. Create indexes during maintenance windows
2. Use online index creation when possible
3. Monitor index build progress
4. Validate index effectiveness

### Connection Pool Updates
1. Update configuration gradually
2. Monitor application performance
3. Rollback if issues occur
4. Document changes

### Read Replica Setup
1. Configure replication
2. Test routing logic
3. Monitor replication lag
4. Implement failover procedures