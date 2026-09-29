ALTER TABLE recurring_transactions
    ADD COLUMN paused_until DATE;

CREATE INDEX idx_recurring_transactions_pause
    ON recurring_transactions(active, paused_until, next_occurrence);
    