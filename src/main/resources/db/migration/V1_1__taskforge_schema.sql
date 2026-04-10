-- =====================================================
-- TaskForge v1.1 - Unified Schema (Consolidated)
-- Includes:
-- - Initial Multi-Tenant Schema (Base V1)
-- - Tenant Invitations (Formerly V2)
-- - Tenant Unique Name Constraint (Formerly V3)
-- - Sprint Management Support (Added)
-- Database: CockroachDB
-- =====================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =====================================================
-- TENANTS
-- =====================================================
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name STRING NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,

    deleted BOOL NOT NULL DEFAULT false
);

CREATE INDEX idx_tenants_active
    ON tenants (id)
    WHERE deleted = false;

-- Add Unique Constraint on Name (From V3)
-- Note: Logic to deduplicate is not needed for fresh installs but good to include if this runs on dirty data?
-- Since this is a consolidated migration intended to replace history, we assume either empty DB or one that fits.
-- However, strict unique constraint definition:
ALTER TABLE tenants ADD CONSTRAINT uk_tenants_name UNIQUE (name);


-- =====================================================
-- USERS (mapped to Keycloak `sub`)
-- =====================================================
CREATE TABLE users (
    id UUID PRIMARY KEY, -- Keycloak sub
    email STRING NOT NULL,
    display_name STRING,
    avatar_url STRING,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,

    deleted BOOL NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX uq_users_email
    ON users (email)
    WHERE deleted = false;

-- =====================================================
-- TENANT MEMBERSHIPS
-- =====================================================
CREATE TABLE tenant_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,

    role STRING NOT NULL, -- OWNER, ADMIN, MEMBER

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,

    deleted BOOL NOT NULL DEFAULT false,

    CONSTRAINT uq_tenant_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_tm_tenant
    ON tenant_memberships (tenant_id);

CREATE INDEX idx_tm_user
    ON tenant_memberships (user_id);

-- =====================================================
-- TENANT INVITATIONS (From V2)
-- =====================================================
CREATE TABLE tenant_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenant_invitations_token ON tenant_invitations(token);
CREATE INDEX idx_tenant_invitations_email ON tenant_invitations(email);


-- =====================================================
-- PROJECTS
-- =====================================================
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    key STRING NOT NULL, -- e.g. PROJ
    name STRING NOT NULL,
    description STRING,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,

    deleted BOOL NOT NULL DEFAULT false,

    CONSTRAINT uq_project_key_per_tenant UNIQUE (tenant_id, key)
);

CREATE INDEX idx_projects_tenant
    ON projects (tenant_id, id)
    WHERE deleted = false;

-- =====================================================
-- PROJECTS: SPRINTS (Added)
-- =====================================================
CREATE TABLE sprints (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,

    name STRING NOT NULL,
    goal STRING,

    start_date TIMESTAMPTZ,
    end_date TIMESTAMPTZ,
    status STRING NOT NULL, -- PLANNED, ACTIVE, COMPLETED

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,

    deleted BOOL NOT NULL DEFAULT false
);

CREATE INDEX idx_sprints_project
    ON sprints (tenant_id, project_id)
    WHERE deleted = false;


-- =====================================================
-- PROJECT MEMBERSHIPS
-- =====================================================
CREATE TABLE project_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    user_id UUID NOT NULL,

    role STRING NOT NULL, -- MANAGER, CONTRIBUTOR, VIEWER

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,

    deleted BOOL NOT NULL DEFAULT false,

    CONSTRAINT uq_project_user UNIQUE (tenant_id, project_id, user_id)
);

CREATE INDEX idx_pm_project
    ON project_memberships (tenant_id, project_id);

CREATE INDEX idx_pm_user
    ON project_memberships (tenant_id, user_id);

-- =====================================================
-- ISSUES
-- =====================================================
CREATE TABLE issues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,

    type STRING NOT NULL,      -- TASK, BUG, STORY
    title STRING NOT NULL,
    description STRING,

    status STRING NOT NULL,    -- TODO, IN_PROGRESS, DONE
    priority STRING NOT NULL,  -- LOW, MEDIUM, HIGH, CRITICAL

    assignee_id UUID,
    reporter_id UUID NOT NULL,

    -- Sprint Support
    sprint_id UUID,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,

    deleted BOOL NOT NULL DEFAULT false
);

CREATE INDEX idx_issues_tenant_project
    ON issues (tenant_id, project_id)
    WHERE deleted = false;

CREATE INDEX idx_issues_tenant_assignee
    ON issues (tenant_id, assignee_id)
    WHERE deleted = false;

CREATE INDEX idx_issues_status
    ON issues (tenant_id, status)
    WHERE deleted = false;

CREATE INDEX idx_issues_sprint
    ON issues (tenant_id, sprint_id)
    WHERE deleted = false;

-- =====================================================
-- COMMENTS
-- =====================================================
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    issue_id UUID NOT NULL,
    author_id UUID NOT NULL,

    body STRING NOT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID,
    updated_at TIMESTAMPTZ,
    updated_by UUID,

    deleted BOOL NOT NULL DEFAULT false
);

CREATE INDEX idx_comments_issue
    ON comments (tenant_id, issue_id)
    WHERE deleted = false;

-- =====================================================
-- OUTBOX EVENTS (Transactional Messaging)
-- =====================================================
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    aggregate_type STRING NOT NULL, -- issue, project
    aggregate_id UUID NOT NULL,

    event_type STRING NOT NULL,
    event_version INT NOT NULL,

    payload JSONB NOT NULL,

    published BOOL NOT NULL DEFAULT false,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_outbox_unpublished
    ON outbox_events (published, created_at)
    WHERE published = false;

CREATE INDEX idx_outbox_tenant
    ON outbox_events (tenant_id);
