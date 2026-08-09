-- V13: Sandbox Foundation

-- 1. Drop the existing NOT NULL constraint on email
ALTER TABLE users
    ALTER COLUMN email DROP NOT NULL;

-- 2. Drop the blanket unique constraint on email
--    (created as column-level UNIQUE in V5, Postgres names it users_email_key)
ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_email_key;

-- 3. Create a partial unique index — uniqueness enforced only for real emails
CREATE UNIQUE INDEX idx_users_email_unique_partial
    ON users(email)
    WHERE email IS NOT NULL;

-- 4. Add sandbox lifecycle columns to organizations
ALTER TABLE organizations
    ADD COLUMN is_sandbox   BOOLEAN   NOT NULL DEFAULT FALSE,
    ADD COLUMN expires_at   TIMESTAMP;

-- 5. Enforce the invariant: if is_sandbox=true then expires_at must be set
ALTER TABLE organizations
    ADD CONSTRAINT organizations_sandbox_expiry_check
        CHECK (
            (is_sandbox = FALSE)
            OR
            (is_sandbox = TRUE AND expires_at IS NOT NULL)
        );

-- 6. Index for the cleanup scheduler query
CREATE INDEX idx_organizations_sandbox_expiry
    ON organizations(expires_at)
    WHERE is_sandbox = TRUE;

-- 7. Add sandbox_token_hash to users for resilience
--    If Redis is unavailable, we can fall back to DB-stored token.
--    Stores HMAC-SHA256 hash of the sandboxToken (never the raw token).
ALTER TABLE users
    ADD COLUMN sandbox_token_hash VARCHAR(255),
    ADD COLUMN sandbox_created_at  TIMESTAMP;

CREATE UNIQUE INDEX idx_users_sandbox_token_hash
    ON users(sandbox_token_hash)
    WHERE sandbox_token_hash IS NOT NULL;