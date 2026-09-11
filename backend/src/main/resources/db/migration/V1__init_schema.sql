-- AI-Powered Enterprise Risk Intelligence Platform: core schema
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================================
-- Identity & Access
-- ============================================================
CREATE TABLE users (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL UNIQUE,
    password_hash     VARCHAR(255) NOT NULL,
    full_name         VARCHAR(255) NOT NULL,
    role              VARCHAR(32)  NOT NULL CHECK (role IN ('ADMIN','SECURITY_ANALYST','EXECUTIVE','VIEWER')),
    business_unit     VARCHAR(128),
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at     TIMESTAMPTZ
);

CREATE TABLE refresh_tokens (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash        VARCHAR(255) NOT NULL UNIQUE,
    expires_at        TIMESTAMPTZ NOT NULL,
    revoked           BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);

-- ============================================================
-- Asset Inventory
-- ============================================================
CREATE TABLE assets (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_tag         VARCHAR(64) NOT NULL UNIQUE,
    name              VARCHAR(255) NOT NULL,
    asset_type        VARCHAR(32) NOT NULL CHECK (asset_type IN ('SERVER','DATABASE','APPLICATION','NETWORK_DEVICE','CLOUD_RESOURCE','ENDPOINT','CONTAINER')),
    business_unit     VARCHAR(128) NOT NULL,
    owner             VARCHAR(255),
    criticality       VARCHAR(16) NOT NULL CHECK (criticality IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    exposure          VARCHAR(16) NOT NULL CHECK (exposure IN ('INTERNAL','DMZ','PUBLIC')),
    environment       VARCHAR(16) NOT NULL CHECK (environment IN ('PRODUCTION','STAGING','DEVELOPMENT','TEST')),
    ip_address        VARCHAR(64),
    hostname          VARCHAR(255),
    tags              JSONB NOT NULL DEFAULT '[]',
    is_active         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_assets_criticality ON assets(criticality);
CREATE INDEX idx_assets_business_unit ON assets(business_unit);
CREATE INDEX idx_assets_exposure ON assets(exposure);

-- ============================================================
-- Vulnerability Management
-- ============================================================
CREATE TABLE vulnerabilities (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id              UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    cve_id                VARCHAR(32),
    title                 VARCHAR(500) NOT NULL,
    description           TEXT,
    cvss_score            NUMERIC(3,1) NOT NULL CHECK (cvss_score >= 0 AND cvss_score <= 10),
    severity              VARCHAR(16) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status                VARCHAR(24) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','IN_PROGRESS','MITIGATED','RESOLVED','ACCEPTED_RISK')),
    exploit_available     BOOLEAN NOT NULL DEFAULT FALSE,
    patch_available       BOOLEAN NOT NULL DEFAULT TRUE,
    discovered_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_date              DATE,
    resolved_at           TIMESTAMPTZ,
    remediation_notes     TEXT,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_vulns_asset ON vulnerabilities(asset_id);
CREATE INDEX idx_vulns_severity ON vulnerabilities(severity);
CREATE INDEX idx_vulns_status ON vulnerabilities(status);
CREATE INDEX idx_vulns_cve ON vulnerabilities(cve_id);

-- ============================================================
-- Security Events (Kafka-ingested)
-- ============================================================
CREATE TABLE security_events (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id          UUID REFERENCES assets(id) ON DELETE SET NULL,
    event_type        VARCHAR(64) NOT NULL,
    severity          VARCHAR(16) NOT NULL CHECK (severity IN ('INFO','LOW','MEDIUM','HIGH','CRITICAL')),
    source_ip         VARCHAR(64),
    destination_ip    VARCHAR(64),
    source_port       INTEGER,
    destination_port  INTEGER,
    protocol          VARCHAR(16),
    description       TEXT,
    raw_payload       JSONB,
    event_time        TIMESTAMPTZ NOT NULL,
    ingested_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    anomaly_score     NUMERIC(6,5),
    is_anomaly        BOOLEAN NOT NULL DEFAULT FALSE,
    scored_at         TIMESTAMPTZ
);
CREATE INDEX idx_events_asset ON security_events(asset_id);
CREATE INDEX idx_events_time ON security_events(event_time DESC);
CREATE INDEX idx_events_anomaly ON security_events(is_anomaly) WHERE is_anomaly = TRUE;
CREATE INDEX idx_events_type ON security_events(event_type);

-- ============================================================
-- Incident Management
-- ============================================================
CREATE TABLE incidents (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_number       VARCHAR(32) NOT NULL UNIQUE,
    title                 VARCHAR(500) NOT NULL,
    description           TEXT,
    severity              VARCHAR(16) NOT NULL CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    status                VARCHAR(24) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','INVESTIGATING','CONTAINED','RESOLVED','CLOSED')),
    related_asset_id      UUID REFERENCES assets(id) ON DELETE SET NULL,
    related_vulnerability_id UUID REFERENCES vulnerabilities(id) ON DELETE SET NULL,
    related_event_id      UUID REFERENCES security_events(id) ON DELETE SET NULL,
    assigned_analyst_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    root_cause            TEXT,
    resolution_summary     TEXT,
    opened_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    contained_at          TIMESTAMPTZ,
    resolved_at           TIMESTAMPTZ,
    closed_at             TIMESTAMPTZ,
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_incidents_status ON incidents(status);
CREATE INDEX idx_incidents_severity ON incidents(severity);
CREATE INDEX idx_incidents_asset ON incidents(related_asset_id);

-- ============================================================
-- Risk Scoring (explainable, versioned per asset)
-- ============================================================
CREATE TABLE risk_scores (
    id                        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    asset_id                  UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    overall_score             NUMERIC(5,2) NOT NULL CHECK (overall_score >= 0 AND overall_score <= 100),
    vulnerability_component   NUMERIC(5,2) NOT NULL,
    exposure_component        NUMERIC(5,2) NOT NULL,
    threat_activity_component NUMERIC(5,2) NOT NULL,
    compliance_component      NUMERIC(5,2) NOT NULL,
    incident_history_component NUMERIC(5,2) NOT NULL,
    risk_tier                 VARCHAR(16) NOT NULL CHECK (risk_tier IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    explanation               JSONB NOT NULL,
    calculated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_risk_scores_asset ON risk_scores(asset_id);
CREATE INDEX idx_risk_scores_calculated_at ON risk_scores(calculated_at DESC);

-- ============================================================
-- Compliance (conceptual NIST CSF / ISO 27001 / SOC 2 mappings)
-- ============================================================
CREATE TABLE compliance_controls (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    framework         VARCHAR(24) NOT NULL CHECK (framework IN ('NIST_CSF','ISO_27001','SOC2')),
    control_id        VARCHAR(32) NOT NULL,
    control_name      VARCHAR(255) NOT NULL,
    control_description TEXT,
    category          VARCHAR(128),
    UNIQUE(framework, control_id)
);

CREATE TABLE compliance_assessments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    control_id        UUID NOT NULL REFERENCES compliance_controls(id) ON DELETE CASCADE,
    business_unit     VARCHAR(128) NOT NULL,
    status            VARCHAR(24) NOT NULL CHECK (status IN ('COMPLIANT','PARTIAL','NON_COMPLIANT','NOT_ASSESSED')),
    evidence_summary  TEXT,
    assessed_by       UUID REFERENCES users(id) ON DELETE SET NULL,
    assessed_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_compliance_assessments_control ON compliance_assessments(control_id);
CREATE INDEX idx_compliance_assessments_bu ON compliance_assessments(business_unit);

-- ============================================================
-- Remediation Recommendations
-- ============================================================
CREATE TABLE remediation_recommendations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vulnerability_id  UUID NOT NULL REFERENCES vulnerabilities(id) ON DELETE CASCADE,
    priority          VARCHAR(16) NOT NULL CHECK (priority IN ('LOW','MEDIUM','HIGH','URGENT')),
    priority_score    NUMERIC(6,2) NOT NULL,
    recommendation    TEXT NOT NULL,
    estimated_effort_hours NUMERIC(6,1),
    status            VARCHAR(24) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACKNOWLEDGED','IN_PROGRESS','COMPLETED','DISMISSED')),
    generated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_remediation_vuln ON remediation_recommendations(vulnerability_id);
CREATE INDEX idx_remediation_priority ON remediation_recommendations(priority);
CREATE INDEX idx_remediation_status ON remediation_recommendations(status);

-- ============================================================
-- Audit Log
-- ============================================================
CREATE TABLE audit_logs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    actor_email       VARCHAR(255),
    action            VARCHAR(128) NOT NULL,
    entity_type       VARCHAR(64) NOT NULL,
    entity_id         VARCHAR(64),
    details           JSONB,
    ip_address        VARCHAR(64),
    occurred_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_logs_occurred_at ON audit_logs(occurred_at DESC);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_user_id);

-- ============================================================
-- RAG Knowledge Base (pgvector embeddings)
-- ============================================================
CREATE TABLE rag_documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type       VARCHAR(32) NOT NULL CHECK (source_type IN ('ASSET','VULNERABILITY','INCIDENT','COMPLIANCE','POLICY','RISK_SCORE')),
    source_id         UUID,
    title             VARCHAR(500) NOT NULL,
    content           TEXT NOT NULL,
    embedding         vector(384),
    metadata          JSONB NOT NULL DEFAULT '{}',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_rag_documents_source ON rag_documents(source_type, source_id);
CREATE INDEX idx_rag_documents_embedding ON rag_documents USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE TABLE rag_conversations (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    question          TEXT NOT NULL,
    answer            TEXT NOT NULL,
    referenced_documents JSONB NOT NULL DEFAULT '[]',
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_rag_conversations_user ON rag_conversations(user_id);
