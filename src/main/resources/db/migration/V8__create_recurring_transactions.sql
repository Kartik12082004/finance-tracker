CREATE TABLE recurring_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    account_id UUID NOT NULL,
    category_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    description VARCHAR(255),
    frequency_unit VARCHAR(20) NOT NULL,
    frequency_interval INTEGER NOT NULL,
    next_occurrence DATE NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_recurring_transactions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_recurring_transactions_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id),

    CONSTRAINT fk_recurring_transactions_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id),

    CONSTRAINT chk_recurring_transactions_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_recurring_transactions_frequency_interval_positive
        CHECK (frequency_interval > 0)
);

CREATE INDEX idx_recurring_transactions_user_id
    ON recurring_transactions(user_id);

CREATE INDEX idx_recurring_transactions_account_id
    ON recurring_transactions(account_id);

CREATE INDEX idx_recurring_transactions_category_id
    ON recurring_transactions(category_id);

CREATE INDEX idx_recurring_transactions_due
    ON recurring_transactions(active, next_occurrence);
    