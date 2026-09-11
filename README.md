# AI-Powered Enterprise Risk Intelligence Platform

An enterprise cybersecurity risk intelligence platform that turns security events,
vulnerabilities, assets, and compliance findings into explainable risk scores, ML-driven
anomaly insights, and prioritized remediation guidance - built as a Deloitte GDS
Technology Analyst portfolio project.

> Security events + vulnerabilities + assets + compliance findings -> risk scores -> AI/ML insights -> remediation recommendations

## Stack

| Layer | Technology |
|---|---|
| Frontend | React 19 + TypeScript + Vite + Tailwind CSS + Recharts |
| Backend | Java 21 + Spring Boot 3 + Spring Security + Spring Data JPA |
| Database | PostgreSQL 16 + pgvector |
| Caching | Redis |
| Messaging | Apache Kafka |
| ML | Python + FastAPI + scikit-learn (Isolation Forest) |
| AI | RAG-based security assistant with a pluggable LLM provider |
| DevOps | Docker Compose + GitHub Actions |

## Repository layout

```
backend/         Spring Boot API (auth, assets, vulnerabilities, risk engine, incidents,
                 compliance, remediation, RAG assistant, analytics)
ml-service/      FastAPI microservice: Isolation Forest anomaly detection + embeddings
frontend/        React/TypeScript SPA: analyst + executive dashboards, AI assistant
scripts/         Synthetic data generator (100+ assets, 300+ vulns, 10k+ events, 50+ incidents)
docs/            Architecture, risk engine, RAG, ML, security, API, interview & resume docs
.github/workflows/ci.yml   GitHub Actions CI (backend/ml-service/frontend build+test)
docker-compose.yml         Full local environment (Postgres, Redis, Kafka, all 3 services)
```

## Quick start (Docker Compose)

```bash
docker compose up --build
```

This starts Postgres (with pgvector), Redis, Kafka, the ML service, the Spring Boot
backend, and the frontend. On first boot Flyway migrations create the schema and seed a
compliance-control catalog plus four demo accounts (password `Passw0rd!123` for all):

| Role | Email |
|---|---|
| Admin | `admin@erip.com` |
| Security Analyst | `analyst@erip.com` |
| Executive | `exec@erip.com` |
| Viewer | `viewer@erip.com` |

Then load realistic demo data:

```bash
pip install -r scripts/requirements.txt
python scripts/generate_synthetic_data.py --database-url postgresql://erip_app:erip_dev_password@localhost:15432/erip
```

Open the app at **http://localhost:3100**. Swagger/OpenAPI docs are at
**http://localhost:8090/swagger-ui.html**.

> Ports are deliberately non-default (15432/6380/8090/3100 instead of 5432/6379/8080/3000)
> so the stack doesn't collide with other projects' containers on the same machine. Override
> any of them via `POSTGRES_HOST_PORT`, `REDIS_HOST_PORT`, `BACKEND_HOST_PORT`, or
> `FRONTEND_HOST_PORT` environment variables before `docker compose up` if you'd rather use
> the defaults and have nothing else running on them.

After loading data, log in as `admin@erip.com` and:
1. Wait for the scheduled risk-engine job (runs every 15 minutes) or call
   `POST /api/v1/risk-scores/assets/{id}/recompute` per asset for immediate scores.
2. Call `POST /api/v1/rag/reindex` to build the RAG knowledge base so the AI assistant
   has grounded records to answer from.

## Running services individually (without Docker)

```bash
# Backend
cd backend && ./mvnw spring-boot:run

# ML service
cd ml-service && pip install -r requirements.txt && uvicorn app.main:app --reload --port 8001

# Frontend
cd frontend && npm install && npm run dev
```

## Tests

```bash
cd backend && ./mvnw test               # unit tests (no Docker required)
cd backend && ./mvnw verify             # + Testcontainers integration tests (requires Docker)
cd ml-service && pytest tests/ -v
cd frontend && npm run test
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md) - system overview and component diagram
- [Database ERD](docs/ERD.md)
- [Kafka event pipeline](docs/KAFKA_PIPELINE.md)
- [Risk engine](docs/RISK_ENGINE.md) - explainable scoring methodology
- [ML anomaly detection model](docs/ML_MODEL.md)
- [RAG assistant flow](docs/RAG_FLOW.md)
- [AWS deployment topology](docs/AWS_DEPLOYMENT.md)
- [API reference](docs/API.md)
- [Security design](docs/SECURITY.md)
- [Interview guide](docs/INTERVIEW_GUIDE.md) - architectural decisions explained
- [Resume description](docs/RESUME_DESCRIPTION.md) - resume bullets for this project

## Role-based access control

| Role | Capabilities |
|---|---|
| `ADMIN` | Full access: user management, all CRUD, RAG reindex |
| `SECURITY_ANALYST` | Manage assets/vulnerabilities/incidents/remediation, analyst dashboard |
| `EXECUTIVE` | Read-only + executive dashboard (enterprise risk, compliance posture) |
| `VIEWER` | Read-only access to assets, vulnerabilities, incidents, compliance |
