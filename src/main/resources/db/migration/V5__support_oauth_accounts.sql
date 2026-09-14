-- OAuth-only users do not have a local password.
ALTER TABLE users
    ALTER COLUMN password_hash DROP NOT NULL;

-- Stores external OAuth identities separately from the main Finance Tracker user.
CREATE TABLE oauth_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_oauth_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uq_oauth_provider_user
        UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_oauth_accounts_user_id
    ON oauth_accounts(user_id);