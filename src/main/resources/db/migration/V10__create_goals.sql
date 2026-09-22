CREATE TABLE goals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    target_amount NUMERIC(19,4) NOT NULL,
    current_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    target_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_goals_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT chk_goals_target_amount_positive
        CHECK (target_amount > 0),

    CONSTRAINT chk_goals_current_amount_non_negative
        CHECK (current_amount >= 0),

    CONSTRAINT chk_goals_current_not_above_target
        CHECK (current_amount <= target_amount)
);

CREATE INDEX idx_goals_user_id
    ON goals(user_id);

CREATE INDEX idx_goals_user_status
    ON goals(user_id, status);

CREATE INDEX idx_goals_user_target_date
    ON goals(user_id, target_date);
    