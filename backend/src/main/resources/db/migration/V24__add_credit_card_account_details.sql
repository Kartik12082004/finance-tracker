ALTER TABLE accounts
    ADD COLUMN credit_limit NUMERIC(19,4),
    ADD COLUMN parent_account_id UUID;

ALTER TABLE accounts
    ADD CONSTRAINT fk_accounts_parent_account
        FOREIGN KEY (parent_account_id)
        REFERENCES accounts(id);

CREATE INDEX idx_accounts_parent_account_id
    ON accounts(parent_account_id);
