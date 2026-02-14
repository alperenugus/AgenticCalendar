-- Migration script to transform appointments table to events table
-- This script handles the migration from appointment-based to event-based calendar system

-- Step 1: Create new events table with all calendar features
CREATE TABLE IF NOT EXISTS events (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(500) NOT NULL,
    description VARCHAR(2000),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    timezone VARCHAR(50) DEFAULT 'UTC',
    type VARCHAR(20) DEFAULT 'MEETING',
    status VARCHAR(20) DEFAULT 'CONFIRMED',
    location VARCHAR(500),
    is_all_day BOOLEAN DEFAULT FALSE,
    recurrence_rule VARCHAR(500),
    reminder_time TIMESTAMP,
    color VARCHAR(20) DEFAULT '#3b82f6',
    session_id VARCHAR(255) NOT NULL,
    google_user_id VARCHAR(255),
    google_user_email VARCHAR(255)
);

-- Step 2: Migrate existing appointments data to events (if appointments table exists)
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'appointments') THEN
        -- Migrate appointments to events
        INSERT INTO events (title, description, start_time, end_time, session_id, status, type)
        SELECT 
            COALESCE(description, 'Untitled Event') as title,
            description,
            appointment_date_time as start_time,
            appointment_date_time + INTERVAL '1 hour' as end_time,  -- Default 1 hour duration
            'migrated-' || COALESCE(user_id::text, 'default') as session_id,
            'CONFIRMED' as status,
            'MEETING' as type
        FROM appointments
        ON CONFLICT DO NOTHING;
        
        RAISE NOTICE 'Migrated appointments to events';
    END IF;
END $$;

-- Step 3: Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_events_session_id ON events(session_id);
CREATE INDEX IF NOT EXISTS idx_events_google_user_id ON events(google_user_id);
CREATE INDEX IF NOT EXISTS idx_events_start_time ON events(start_time);
CREATE INDEX IF NOT EXISTS idx_events_status ON events(status);
CREATE INDEX IF NOT EXISTS idx_events_start_end_time ON events(start_time, end_time);

-- Note: The old appointments table will be kept for reference but can be dropped later
-- DROP TABLE IF EXISTS appointments;  -- Uncomment after verifying migration

