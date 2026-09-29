ALTER TABLE recurring_transactions
    ADD COLUMN anchor_month INTEGER,
    ADD COLUMN anchor_day INTEGER;

-- Existing recurring rules use their current next occurrence
-- as the best available calendar anchor.
UPDATE recurring_transactions
SET
    anchor_month = EXTRACT(MONTH FROM next_occurrence),
    anchor_day = EXTRACT(DAY FROM next_occurrence);

ALTER TABLE recurring_transactions
    ALTER COLUMN anchor_month SET NOT NULL,
    ALTER COLUMN anchor_day SET NOT NULL;

ALTER TABLE recurring_transactions
    ADD CONSTRAINT chk_recurring_anchor_month
        CHECK (anchor_month BETWEEN 1 AND 12),
    ADD CONSTRAINT chk_recurring_anchor_day
        CHECK (anchor_day BETWEEN 1 AND 31);
        