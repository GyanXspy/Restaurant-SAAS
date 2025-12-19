-- Order Service Database Indexes
-- Optimizing query patterns for event sourcing and CQRS read models

-- Indexes for order_events table (Event Store)
CREATE INDEX IF NOT EXISTS idx_order_events_aggregate_id ON order_events(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_order_events_aggregate_version ON order_events(aggregate_id, event_version);
CREATE INDEX IF NOT EXISTS idx_order_events_type ON order_events(event_type);
CREATE INDEX IF NOT EXISTS idx_order_events_created_at ON order_events(created_at);

-- Indexes for order_read_model table (CQRS Read Model)
CREATE INDEX IF NOT EXISTS idx_order_read_customer_created ON order_read_model(customer_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_read_restaurant_created ON order_read_model(restaurant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_read_status ON order_read_model(status);
CREATE INDEX IF NOT EXISTS idx_order_read_status_created ON order_read_model(status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_read_created_at ON order_read_model(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_read_updated_at ON order_read_model(updated_at DESC);

-- Composite index for customer order history with status filtering
CREATE INDEX IF NOT EXISTS idx_order_read_customer_status_created ON order_read_model(customer_id, status, created_at DESC);

-- Composite index for restaurant orders with status filtering
CREATE INDEX IF NOT EXISTS idx_order_read_restaurant_status_created ON order_read_model(restaurant_id, status, created_at DESC);

-- Index for total amount range queries (analytics)
CREATE INDEX IF NOT EXISTS idx_order_read_amount ON order_read_model(total_amount);

-- Saga state table indexes (if exists)
CREATE INDEX IF NOT EXISTS idx_saga_state_order_id ON order_saga_state(order_id) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'order_saga_state');
CREATE INDEX IF NOT EXISTS idx_saga_state_status ON order_saga_state(status) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'order_saga_state');
CREATE INDEX IF NOT EXISTS idx_saga_state_created_at ON order_saga_state(created_at) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'order_saga_state');