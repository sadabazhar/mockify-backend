-- V10__api_keys_and_permissions.sql
-- API Key authentication

-- 0. ENUM DEFINITIONS
-- Permission levels for API keys (hierarchical: read < write < delete < admin)
CREATE TYPE api_permission AS ENUM (
    'READ',     -- View resources
    'WRITE',    -- Create/update resources
    'DELETE',   -- Remove resources
    'ADMIN'     -- Full control (implies all above)
);

-- Resource types that permissions apply to
CREATE TYPE api_resource_type AS ENUM (
    'SCHEMA',
    'RECORD',
    'PROJECT',
    'ORGANIZATION'
);

-- Ensure composite uniqueness on projects
ALTER TABLE projects
ADD CONSTRAINT projects_id_org_unique
UNIQUE (id, organization_id);

-- 1. API KEYS TABLE
-- Stores API keys for programmatic access to APIs
CREATE TABLE api_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Key identification
    name VARCHAR(255) NOT NULL,
    description TEXT,
    key_prefix VARCHAR(16) NOT NULL,  -- e.g., "mk_live_" for display
    key_hash VARCHAR(255) NOT NULL,   -- HMAC-SHA256 hash of full key

    -- Ownership & Scope
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_by UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Optional project-level scoping (NULL = org-wide access)
    project_id UUID,

    -- Key lifecycle
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW(),

    -- Rate limiting (per API key)
    rate_limit_per_minute INTEGER NOT NULL DEFAULT 1000,

    -- Ensure project belongs to organization
    CONSTRAINT fk_api_keys_project_org
        FOREIGN KEY (project_id, organization_id)
        REFERENCES projects(id, organization_id)
        ON DELETE CASCADE,

    -- Prevent invalid lifecycle states (expired before creation).
    CONSTRAINT api_keys_expires_after_created
    CHECK (
        expires_at IS NULL
        OR expires_at > created_at
    ),

    -- Prevent invalid rate limits
    CONSTRAINT api_keys_rate_limit_bounds
        CHECK (rate_limit_per_minute > 0 AND rate_limit_per_minute <= 100000)
);

-- Fast lookup by unique key_hash
CREATE UNIQUE INDEX idx_api_keys_hash_org_unique
    ON api_keys(organization_id, key_hash);

-- Composite index for fast active key lookup (primary authentication query)
CREATE INDEX idx_api_keys_auth_lookup
    ON api_keys(organization_id, key_hash, expires_at)
    WHERE is_active = TRUE;

-- Lookup keys by organization
CREATE INDEX idx_api_keys_organization
    ON api_keys(organization_id);

-- Lookup keys by project
CREATE INDEX idx_api_keys_project
    ON api_keys(project_id)
    WHERE project_id IS NOT NULL;


-- 2. API KEY PERMISSIONS TABLE
-- Defines what actions an API key can perform
CREATE TABLE api_key_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    api_key_id UUID NOT NULL REFERENCES api_keys(id) ON DELETE CASCADE,

    -- Permission types: 'read', 'write', 'delete', 'admin'
    permission api_permission NOT NULL,

    -- Resource scope: 'schema', 'record', 'project', 'organization'
    resource_type api_resource_type NOT NULL,

    -- Optional: specific resource ID (NULL = all resources of this type)
    -- Handle resource validation in application logic.
    resource_id UUID,

    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enforce uniqueness when resource_id IS NULL (wildcard permissions)
CREATE UNIQUE INDEX api_key_permissions_unique_wildcard
    ON api_key_permissions (api_key_id, permission, resource_type)
    WHERE resource_id IS NULL;

-- Enforce uniqueness when resource_id IS NOT NULL (scoped permissions)
CREATE UNIQUE INDEX api_key_permissions_unique_scoped
    ON api_key_permissions (api_key_id, permission, resource_type, resource_id)
    WHERE resource_id IS NOT NULL;

-- Fast permission lookups
CREATE INDEX idx_api_key_permissions_key
    ON api_key_permissions(api_key_id);

CREATE INDEX idx_api_key_permissions_lookup
    ON api_key_permissions (api_key_id, resource_type, resource_id);

