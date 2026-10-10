
-- Add the dedicated account reference to each goal.
-- Keep it nullable temporarily so existing goals can be backfilled.

ALTER TABLE goals
    ADD COLUMN account_id UUID;

-- Create an explicit mapping for this migration.
-- The mapping is based on goal IDs, never goal names.
CREATE TEMPORARY TABLE goal_account_migration_map
ON COMMIT DROP
AS
SELECT
    id AS goal_id,
    gen_random_uuid() AS account_id
FROM goals;

-- Create one zero-balance account for every existing goal.
INSERT INTO accounts (
    id,
    user_id,
    name,
    type,
    currency,
    opening_balance,
    created_at,
    updated_at
)
SELECT
    mapping.account_id,
    goal.user_id,
    'Goal - ' || goal.name,
    'GOAL',
    'INR',
    0.00,
    goal.created_at,
    goal.updated_at
FROM goals goal
JOIN goal_account_migration_map mapping
    ON mapping.goal_id = goal.id;

-- Link each goal to the exact account created for it.
UPDATE goals goal
SET account_id = mapping.account_id
FROM goal_account_migration_map mapping
WHERE mapping.goal_id = goal.id;

-- Enforce the relationship after the backfill is complete.
ALTER TABLE goals
    ALTER COLUMN account_id SET NOT NULL;

ALTER TABLE goals
    ADD CONSTRAINT fk_goals_account
        FOREIGN KEY (account_id) REFERENCES accounts(id);

ALTER TABLE goals
    ADD CONSTRAINT uk_goals_account
        UNIQUE (account_id);
