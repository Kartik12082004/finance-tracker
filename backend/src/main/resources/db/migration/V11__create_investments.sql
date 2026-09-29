CREATE TABLE investments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(20) NOT NULL,
    quantity NUMERIC(19,8),
    average_purchase_price NUMERIC(19,4),
    current_value NUMERIC(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_investments_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT chk_investments_quantity_positive
        CHECK (quantity IS NULL OR quantity > 0),

    CONSTRAINT chk_investments_average_price_positive
        CHECK (
            average_purchase_price IS NULL
            OR average_purchase_price > 0
        ),

    CONSTRAINT chk_investments_current_value_non_negative
        CHECK (current_value >= 0)
);

CREATE TABLE investment_transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    investment_id UUID NOT NULL,
    account_id UUID NOT NULL,
    type VARCHAR(10) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    quantity NUMERIC(19,8),
    price_per_unit NUMERIC(19,4),
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_investment_transactions_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_investment_transactions_investment
        FOREIGN KEY (investment_id)
        REFERENCES investments(id),

    CONSTRAINT fk_investment_transactions_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(id),

    CONSTRAINT chk_investment_transactions_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_investment_transactions_quantity_positive
        CHECK (
            quantity IS NULL
            OR quantity > 0
        ),

    CONSTRAINT chk_investment_transactions_price_positive
        CHECK (
            price_per_unit IS NULL
            OR price_per_unit > 0
        )
);

CREATE INDEX idx_investments_user_id
    ON investments(user_id);

CREATE INDEX idx_investment_transactions_user_id
    ON investment_transactions(user_id);

CREATE INDEX idx_investment_transactions_investment_id
    ON investment_transactions(investment_id);

CREATE INDEX idx_investment_transactions_account_id
    ON investment_transactions(account_id);

CREATE INDEX idx_investment_transactions_occurred_at
    ON investment_transactions(occurred_at);
    