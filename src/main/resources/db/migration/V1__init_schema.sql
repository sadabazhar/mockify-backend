-- USERS
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- ORGANIZATIONS
CREATE TABLE organizations (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    owner_id INT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- PROJECTS
CREATE TABLE projects (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    organization_id INT REFERENCES organizations(id),
    created_at TIMESTAMP DEFAULT NOW()
);

-- USER DEFINED SCHEMAS
CREATE TABLE mock_schemas (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    project_id INT REFERENCES projects(id),
    schema_json JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- USER DEFINED RECORDS
CREATE TABLE mock_records (
    id SERIAL PRIMARY KEY,
    mock_schema_id INT REFERENCES mock_schemas(id),
    data JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);
