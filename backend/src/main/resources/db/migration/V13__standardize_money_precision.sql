ALTER TABLE accounts
    ALTER COLUMN opening_balance TYPE NUMERIC(19,2);

ALTER TABLE transactions
    ALTER COLUMN amount TYPE NUMERIC(19,2);

ALTER TABLE budgets
    ALTER COLUMN amount TYPE NUMERIC(19,2);

ALTER TABLE recurring_transactions
    ALTER COLUMN amount TYPE NUMERIC(19,2);

ALTER TABLE goals
    ALTER COLUMN target_amount TYPE NUMERIC(19,2),
    ALTER COLUMN current_amount TYPE NUMERIC(19,2);
    