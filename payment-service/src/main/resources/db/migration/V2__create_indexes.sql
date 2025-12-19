-- Payment Service Database Indexes
-- Optimizing query patterns for event sourcing and payment processing

-- Indexes for payment_events table (Event Store)
CREATE INDEX IF NOT EXISTS idx_payment_events_aggregate_id ON payment_events(aggregate_id);
CREATE INDEX IF NOT EXISTS idx_payment_events_aggregate_version ON payment_events(aggregate_id, event_version);
CREATE INDEX IF NOT EXISTS idx_payment_events_type ON payment_events(event_type);
CREATE INDEX IF NOT EXISTS idx_payment_events_created_at ON payment_events(created_at);

-- Indexes for payments table (if exists as read model)
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments');
CREATE INDEX IF NOT EXISTS idx_payments_customer_id ON payments(customer_id) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments');
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments');
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments');
CREATE INDEX IF NOT EXISTS idx_payments_amount ON payments(amount) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments');

-- Composite indexes for common query patterns
CREATE INDEX IF NOT EXISTS idx_payments_customer_status_created ON payments(customer_id, status, created_at DESC) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments');
CREATE INDEX IF NOT EXISTS idx_payments_status_created ON payments(status, created_at DESC) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments');

-- Index for transaction ID lookups
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON payments(transaction_id) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments');

-- Index for payment method analytics
CREATE INDEX IF NOT EXISTS idx_payments_method_created ON payments(payment_method, created_at DESC) WHERE EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payments');