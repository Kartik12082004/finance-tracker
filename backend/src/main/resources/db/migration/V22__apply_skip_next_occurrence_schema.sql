ALTER TABLE recurring_transactions
    DROP COLUMN IF EXISTS next_occurrence_before_pause,
    DROP COLUMN IF EXISTS paused_until,
    ADD COLUMN IF NOT EXISTS skip_next_occurrence BOOLEAN NOT NULL DEFAULT FALSE;
