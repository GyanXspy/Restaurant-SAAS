-- Generic Event Store Schema
-- This script creates the events table that can be used by any service
-- for event sourcing implementation

CREATE TABLE IF NOT EXISTS events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data JSON NOT NULL,
    event_version INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for performance
    INDEX idx_aggregate_id (aggregate_id),
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at),
    INDEX idx_aggregate_version (aggregate_id, event_version),
    
    -- Ensure event ordering within aggregate
    UNIQUE KEY uk_aggregate_version (aggregate_id, event_version)
);

-- Table for tracking processed events to ensure idempotency
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    processed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Index for cleanup operations
    INDEX idx_processed_at (processed_at)
);

-- Table for tracking failed events from dead letter queue
CREATE TABLE IF NOT EXISTS failed_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_data TEXT NOT NULL,
    failure_reason TEXT,
    attempt_count INT NOT NULL DEFAULT 1,
    failed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Indexes for querying and analysis
    INDEX idx_event_id (event_id),
    INDEX idx_event_type (event_type),
    INDEX idx_failed_at (failed_at),
    INDEX idx_attempt_count (attempt_count)
);

-- Optional: Create a view for easier event querying
CREATE OR REPLACE VIEW event_summary AS
SELECT 
    aggregate_id,
    event_type,
    COUNT(*) as event_count,
    MIN(created_at) as first_event,
    MAX(created_at) as last_event,
    MAX(event_version) as current_version
FROM events 
GROUP BY aggregate_id, event_type;