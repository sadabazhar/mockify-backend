-- ==========================================
-- Seed Data for Mockify
-- ==========================================

-- 1. Users
INSERT INTO users (name, email, password, created_at)
VALUES
('Vhagar', 'vhagar@mockify.com', 'hashedpassword1', NOW()),
('Caraxes', 'r@example.com', 'hashedpassword2', NOW()),
('Drogon', 'vermithor@example.com', 'hashedpassword3', NOW());

-- 2. Organizations
INSERT INTO organizations (name, owner_id, created_at)
VALUES
('Targaryen', 1, NOW()),
('Stark', 2, NOW()),
('Mormont', 3, NOW());

-- 3. Projects
INSERT INTO projects (name, organization_id, created_at)
VALUES
('Website Redesign', 1, NOW()),
('Mobile App', 1, NOW()),
('Marketing Campaign', 2, NOW()),
('Data Analytics', 3, NOW());

-- 4. Mock Schemas
INSERT INTO mock_schemas (name, project_id, schema_json, created_at)
VALUES
('User API', 1, '{"type": "object", "properties": {"id": {"type": "integer"}, "name": {"type": "string"}, "email": {"type": "string"}}}', NOW()),
('Product API', 2, '{"type": "object", "properties": {"id": {"type": "integer"}, "title": {"type": "string"}, "price": {"type": "number"}}}', NOW()),
('Campaign API', 3, '{"type": "object", "properties": {"id": {"type": "integer"}, "campaignName": {"type": "string"}, "budget": {"type": "number"}}}', NOW()),
('Analytics API', 4, '{"type": "object", "properties": {"id": {"type": "integer"}, "metric": {"type": "string"}, "value": {"type": "number"}}}', NOW());

-- 5. Mock Records
INSERT INTO mock_records (mock_schema_id, data, created_at, expires_at)
VALUES
(1, '{"id": 1, "name": "Alice Johnson", "email": "alice@example.com"}', NOW(), NOW() + INTERVAL '24 hours'),
(1, '{"id": 2, "name": "Bob Smith", "email": "bob@example.com"}', NOW(), NOW() + INTERVAL '24 hours'),
(2, '{"id": 1, "title": "Redesign Homepage", "price": 199.99}', NOW(), NOW() + INTERVAL '24 hours'),
(3, '{"id": 1, "campaignName": "Black Friday", "budget": 5000}', NOW(), NOW() + INTERVAL '24 hours'),
(4, '{"id": 1, "metric": "visits", "value": 1200}', NOW(), NOW() + INTERVAL '24 hours');

