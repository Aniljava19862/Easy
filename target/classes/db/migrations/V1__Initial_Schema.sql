CREATE TABLE app_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) DEFAULT 'USER'
);

CREATE TABLE ui_designs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    json_definition JSONB, -- PostgreSQL JSONB type
    created_by_user_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE table_metadata (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    created_by_user_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE column_metadata (
    id BIGSERIAL PRIMARY KEY,
    table_id BIGINT NOT NULL REFERENCES table_metadata(id) ON DELETE CASCADE,
    column_name VARCHAR(255) NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    length INTEGER,
    is_nullable BOOLEAN NOT NULL,
    is_primary_key BOOLEAN NOT NULL,
    is_unique BOOLEAN NOT NULL,
    is_foreign_key BOOLEAN NOT NULL,
    references_table_id BIGINT,
    references_column_name VARCHAR(255),
    UNIQUE (table_id, column_name)
);

CREATE TABLE custom_logic (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    code_content TEXT,
    language VARCHAR(50) NOT NULL,
    created_by_user_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE job_definitions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    job_type VARCHAR(50) NOT NULL,
    associated_logic_id BIGINT,
    cron_expression VARCHAR(255),
    enabled BOOLEAN NOT NULL,
    created_by_user_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

CREATE TABLE job_executions (
    id BIGSERIAL PRIMARY KEY,
    job_definition_id BIGINT NOT NULL REFERENCES job_definitions(id) ON DELETE CASCADE,
    start_time TIMESTAMP WITHOUT TIME ZONE,
    end_time TIMESTAMP WITHOUT TIME ZONE,
    status VARCHAR(50) NOT NULL,
    logs TEXT,
    error_message TEXT
);

CREATE TABLE workflow_definitions (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    bpmn_xml TEXT,
    process_definition_key VARCHAR(255) UNIQUE,
    created_by_user_id BIGINT,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- Quartz tables will be created by Quartz itself if spring.quartz.jdbc.initialize-schema=always
-- Activiti tables will be created by Activiti itself if spring.activiti.database-schema-update=true