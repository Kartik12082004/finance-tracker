ALTER TABLE recurring_transactions
    DROP CONSTRAINT IF EXISTS chk_recurring_anchor_month;

ALTER TABLE recurring_transactions
    DROP COLUMN IF EXISTS anchor_month;
    