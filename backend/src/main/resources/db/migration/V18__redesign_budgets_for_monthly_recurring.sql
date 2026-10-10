DROP TABLE budgets;

CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    amount NUMERIC(19,2) NOT NULL,
    budget_year INTEGER NOT NULL,
    budget_month INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_budgets_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_budgets_category
        FOREIGN KEY (category_id)
        REFERENCES categories(id),

    CONSTRAINT chk_budgets_amount_positive
        CHECK (amount > 0),

    CONSTRAINT chk_budgets_month_valid
        CHECK (budget_month BETWEEN 1 AND 12),

    CONSTRAINT uq_budgets_user_category_month
        UNIQUE (user_id, category_id, budget_year, budget_month)
);

CREATE INDEX idx_budgets_user_id
    ON budgets(user_id);

CREATE INDEX idx_budgets_category_id
    ON budgets(category_id);

CREATE INDEX idx_budgets_user_month
    ON budgets(user_id, budget_year, budget_month);
    