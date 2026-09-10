CREATE TABLE categories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    parent_id UUID,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    is_default BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id)
        REFERENCES users(id),

    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id)
        REFERENCES categories(id)
);

CREATE INDEX idx_categories_user_id
    ON categories(user_id);

CREATE INDEX idx_categories_parent_id
    ON categories(parent_id);