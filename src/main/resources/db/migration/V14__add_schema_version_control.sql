-- Add version tracking columns to mock_schemas table
ALTER TABLE mock_schemas ADD COLUMN IF NOT EXISTS active_version INT DEFAULT 1 NOT NULL;
ALTER TABLE mock_schemas ADD COLUMN IF NOT EXISTS lock_version BIGINT DEFAULT 0 NOT NULL;

-- Create mock_schema_versions table for snapshot history
CREATE TABLE IF NOT EXISTS mock_schema_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mock_schema_id UUID NOT NULL,
    version INT NOT NULL,
    schema_json_snapshot JSONB NOT NULL,
    diff_json JSONB,
    commit_message VARCHAR(500),
    changed_by_user_id UUID,
    created_at TIMESTAMP DEFAULT NOW() NOT NULL,
    CONSTRAINT fk_schema_versions_parent FOREIGN KEY (mock_schema_id) REFERENCES mock_schemas(id) ON DELETE CASCADE,
    CONSTRAINT fk_schema_versions_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Ensure version uniqueness per schema
ALTER TABLE mock_schema_versions ADD CONSTRAINT uq_schema_version UNIQUE (mock_schema_id, version);

-- Performance indices
CREATE UNIQUE INDEX IF NOT EXISTS idx_schema_versions_composite ON mock_schema_versions(mock_schema_id, version DESC);
CREATE INDEX IF NOT EXISTS idx_schema_versions_audit ON mock_schema_versions(changed_by_user_id, created_at);

-- Backfill existing schemas as Version 1
INSERT INTO mock_schema_versions (mock_schema_id, version, schema_json_snapshot, commit_message, created_at)
SELECT id, 1, schema_json, 'Initial baseline snapshot generated during migration V13', NOW()
FROM mock_schemas
ON CONFLICT (mock_schema_id, version) DO NOTHING;
