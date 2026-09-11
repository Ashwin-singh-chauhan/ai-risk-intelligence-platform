# Resume Description

Bullets below describe what was actually built in this repository - they avoid invented
metrics (no fabricated "% reduction in incident response time," no fake user counts). If
you deploy this and use it against a real workload with real usage, replace the italicized
placeholders with genuine measured numbers.

## One-line summary

**AI-Powered Enterprise Risk Intelligence Platform** - a full-stack cybersecurity risk
platform (Java/Spring Boot, Python/FastAPI, React/TypeScript) that converts security
events, vulnerabilities, and compliance findings into an explainable 0-100 risk score,
ML-based anomaly detection, and a RAG-grounded AI assistant.

## Full project bullet set (pick 3-5 for a resume, use all for a portfolio page)

- Designed and built a full-stack enterprise risk intelligence platform (Java 21/Spring
  Boot 3 backend, Python/FastAPI ML service, React 19/TypeScript frontend) modeling
  asset inventory, vulnerability management, security event ingestion, incident
  management, and compliance tracking across NIST CSF, ISO 27001, and SOC 2.

- Designed an explainable, weighted risk-scoring engine that combines vulnerability
  severity, asset exposure/criticality, recent threat activity, compliance posture, and
  incident history into a 0-100 score with a per-component, human-readable
  justification persisted alongside every calculation - not a black-box model output.

- Built a Kafka-based event ingestion pipeline (producer/consumer, dead-letter topic,
  fail-soft ML integration) that asynchronously routes security events through an
  Isolation Forest anomaly-detection model served by a separate FastAPI microservice.

- Implemented a retrieval-augmented generation (RAG) security assistant using
  PostgreSQL/pgvector for similarity search over live platform data (assets,
  vulnerabilities, incidents, risk scores), with a provider-agnostic LLM abstraction
  supporting both an offline deterministic mode and OpenAI-compatible chat completion
  APIs.

- Implemented JWT-based authentication with refresh-token rotation and four-role RBAC
  (Admin, Security Analyst, Executive, Viewer) enforced via method-level authorization,
  backed by a centralized audit log covering every mutating operation across the system.

- Wrote unit tests validating the risk-scoring and remediation-prioritization algorithms'
  monotonicity properties (e.g. exploited/unpatched vulnerabilities must score higher),
  a Testcontainers-backed integration test exercising the full HTTP-to-database stack
  including RBAC enforcement, and a pytest suite validating the ML service's anomaly
  ranking and embedding similarity behavior.

- Containerized the full stack (Postgres+pgvector, Redis, Kafka, backend, ML service,
  frontend) with Docker Compose and authored a GitHub Actions CI pipeline building and
  testing all three services plus their container images on every push.

- Authored a synthetic-data generator producing *(112 assets, 350 vulnerabilities, 12,000
  security events, 60 incidents in the default configuration - adjust to match your run)*
  realistic demo data to exercise the dashboards, risk engine, and AI assistant end-to-end.

## Shorter variants by focus area

**Backend/API focus:**
> Built a Spring Boot 3 REST API with JWT/RBAC authentication, Kafka event ingestion,
> Flyway-managed PostgreSQL schema (including pgvector for similarity search), and an
> explainable rule-based risk-scoring engine, covered by JUnit unit tests and a
> Testcontainers integration test suite.

**ML/AI focus:**
> Built a RAG-grounded AI security assistant (pgvector similarity search + pluggable LLM
> provider) and an Isolation Forest anomaly-detection microservice (FastAPI +
> scikit-learn) integrated into a Kafka-based event pipeline, with feature engineering
> and model behavior validated by an automated pytest suite.

**Full-stack focus:**
> Designed and implemented a full-stack enterprise security platform end-to-end - React/
> TypeScript dashboards, a Java/Spring Boot API with RBAC and an explainable risk engine,
> and a Python ML microservice - containerized with Docker Compose and built/tested via
> GitHub Actions CI.

## Notes on truthfulness

- Do not add uptime, user-adoption, or performance-improvement metrics unless you have
  actually measured them against a real deployment - none are claimed here because none
  exist yet for a freshly built portfolio project.
- The synthetic data volumes above are the *defaults built into the generator script*
  (`scripts/generate_synthetic_data.py --assets 120 --vulnerabilities 350 --events 12000
  --incidents 60`), which are true and verifiable by running it, not marketing rounding.
- If asked in an interview, be ready to say plainly which parts are demo-scoped trade-offs
  (synthetic ML training baseline, HashingVectorizer embeddings, mock LLM as default) -
  see [INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md). Naming trade-offs precisely reads as more
  senior than implying everything here is production-hardened.
