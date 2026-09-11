# Database Entity-Relationship Diagram

Schema source of truth: `backend/src/main/resources/db/migration/V1__init_schema.sql`
(Flyway-versioned; V2 seeds the compliance control catalog, V3 seeds demo users).

```mermaid
erDiagram
    USERS ||--o{ REFRESH_TOKENS : issues
    USERS ||--o{ INCIDENTS : "assigned to"
    USERS ||--o{ COMPLIANCE_ASSESSMENTS : assesses
    USERS ||--o{ RAG_CONVERSATIONS : asks

    ASSETS ||--o{ VULNERABILITIES : has
    ASSETS ||--o{ SECURITY_EVENTS : generates
    ASSETS ||--o{ RISK_SCORES : "scored for"
    ASSETS ||--o{ INCIDENTS : "related to"

    VULNERABILITIES ||--o{ REMEDIATION_RECOMMENDATIONS : generates
    VULNERABILITIES ||--o{ INCIDENTS : "related to"
    SECURITY_EVENTS ||--o{ INCIDENTS : "related to"

    COMPLIANCE_CONTROLS ||--o{ COMPLIANCE_ASSESSMENTS : "assessed via"

    USERS {
        uuid id PK
        string email UK
        string password_hash
        string role
        string business_unit
        boolean is_active
    }

    ASSETS {
        uuid id PK
        string asset_tag UK
        string name
        string asset_type
        string business_unit
        string criticality
        string exposure
        string environment
    }

    VULNERABILITIES {
        uuid id PK
        uuid asset_id FK
        string cve_id
        numeric cvss_score
        string severity
        string status
        boolean exploit_available
        boolean patch_available
    }

    SECURITY_EVENTS {
        uuid id PK
        uuid asset_id FK
        string event_type
        string severity
        timestamptz event_time
        numeric anomaly_score
        boolean is_anomaly
    }

    INCIDENTS {
        uuid id PK
        string incident_number UK
        string severity
        string status
        uuid related_asset_id FK
        uuid related_vulnerability_id FK
        uuid related_event_id FK
        uuid assigned_analyst_id FK
    }

    RISK_SCORES {
        uuid id PK
        uuid asset_id FK
        numeric overall_score
        numeric vulnerability_component
        numeric exposure_component
        numeric threat_activity_component
        numeric compliance_component
        numeric incident_history_component
        string risk_tier
        jsonb explanation
        timestamptz calculated_at
    }

    COMPLIANCE_CONTROLS {
        uuid id PK
        string framework
        string control_id
        string control_name
    }

    COMPLIANCE_ASSESSMENTS {
        uuid id PK
        uuid control_id FK
        string business_unit
        string status
        uuid assessed_by FK
    }

    REMEDIATION_RECOMMENDATIONS {
        uuid id PK
        uuid vulnerability_id FK
        string priority
        numeric priority_score
        string status
    }

    RAG_DOCUMENTS {
        uuid id PK
        string source_type
        uuid source_id
        text content
        vector embedding
    }

    RAG_CONVERSATIONS {
        uuid id PK
        uuid user_id FK
        text question
        text answer
        jsonb referenced_documents
    }

    AUDIT_LOGS {
        uuid id PK
        uuid actor_user_id FK
        string action
        string entity_type
        string entity_id
        jsonb details
    }
```

## Notable design choices

- **UUID primary keys everywhere** - avoids sequential-ID enumeration across tenants/
  environments and lets the synthetic-data generator and the app both mint IDs client-side.
- **`risk_scores` is append-only, not upserted** - every recomputation inserts a new row,
  so `docs/RISK_ENGINE.md`'s trend charts and audit trail come for free from `calculated_at`.
- **`rag_documents.embedding` is `vector(384)`** (pgvector) with an IVFFlat cosine index -
  see [docs/RAG_FLOW.md](RAG_FLOW.md) for why 384 dimensions and why JDBC rather than JPA
  is used for this one table.
- **`audit_logs` has no foreign-key `ON DELETE CASCADE` other than `SET NULL`** on the
  actor - audit history must outlive the user account that generated it.
