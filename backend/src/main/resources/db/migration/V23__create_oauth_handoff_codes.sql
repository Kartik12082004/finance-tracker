
CREATE TABLE oauth_handoff_codes (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    session_id_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_oauth_handoff_codes_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT uq_oauth_handoff_codes_hash
        UNIQUE (code_hash)
);

CREATE INDEX idx_oauth_handoff_codes_expiry
    ON oauth_handoff_codes(expires_at);
