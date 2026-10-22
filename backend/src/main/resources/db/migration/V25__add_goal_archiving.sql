ALTER TABLE goals
    ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_goals_user_archived
    ON goals(user_id, archived);
