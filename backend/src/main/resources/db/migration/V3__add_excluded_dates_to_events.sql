-- Add excluded_dates column to events table for Google Calendar-style recurrence handling
-- This column stores comma-separated ISO date strings for deleted instances of recurring events

ALTER TABLE events ADD COLUMN IF NOT EXISTS excluded_dates VARCHAR(2000);

-- Add index for better query performance when filtering by excluded dates
CREATE INDEX IF NOT EXISTS idx_events_recurrence_rule ON events(recurrence_rule) WHERE recurrence_rule IS NOT NULL;

