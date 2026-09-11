# API Reference

> Examples below use port 8080 (the backend's default when run directly via
> `./mvnw spring-boot:run` or `java -jar`). Under `docker compose up`, the backend's host
> port is remapped to 8090 by default to avoid clashing with other local projects - see
> the root [README](../README.md#quick-start-docker-compose). Swap the port accordingly.

Full interactive documentation (request/response schemas, try-it-out) is auto-generated
by springdoc-openapi and served at:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Raw OpenAPI JSON: `http://localhost:8080/v3/api-docs`

All endpoints are versioned under `/api/v1`. Authentication is JWT bearer (`Authorization:
Bearer <token>`), obtained from `POST /api/v1/auth/login`.

## Endpoint groups

| Base path | Purpose | Key roles |
|---|---|---|
| `/api/v1/auth` | Login, refresh, logout | Public |
| `/api/v1/users` | User/RBAC administration | `ADMIN` |
| `/api/v1/assets` | Asset inventory CRUD + search | Read: all; Write: `ADMIN`, `SECURITY_ANALYST` |
| `/api/v1/vulnerabilities` | Vulnerability CRUD + status transitions | Read: all; Write: `ADMIN`, `SECURITY_ANALYST` |
| `/api/v1/events` | Kafka event ingestion + query + anomaly list | Ingest: `ADMIN`, `SECURITY_ANALYST`; Read: all |
| `/api/v1/risk-scores` | Latest/history risk scores, on-demand recompute | Read: all; Recompute: `ADMIN`, `SECURITY_ANALYST` |
| `/api/v1/incidents` | Incident lifecycle | Read: all; Write: `ADMIN`, `SECURITY_ANALYST` |
| `/api/v1/compliance` | Control catalog, assessments, posture | Read: all; Assess: `ADMIN`, `SECURITY_ANALYST` |
| `/api/v1/remediations` | Prioritized remediation recommendations | Read: all; Update: `ADMIN`, `SECURITY_ANALYST` |
| `/api/v1/rag` | AI assistant Q&A, reindex | Ask: all; Reindex: `ADMIN` |
| `/api/v1/analytics` | Analyst/executive dashboard aggregates | `ADMIN`+`SECURITY_ANALYST` / `ADMIN`+`EXECUTIVE` |

## Pagination

List endpoints accept standard Spring Data `Pageable` query parameters (`page`, `size`,
`sort=field,direction`) and return a uniform envelope:

```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 347,
  "totalPages": 18,
  "last": false
}
```

## Error format

All errors (validation, not-found, auth, unexpected) return a consistent shape from
`GlobalExceptionHandler`:

```json
{
  "timestamp": "2026-09-12T08:15:30Z",
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "path": "/api/v1/assets",
  "fieldErrors": { "assetTag": "must not be blank" }
}
```

## Example: end-to-end curl walkthrough

```bash
# 1. Log in
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"analyst@erip.com","password":"Passw0rd!123"}' | jq -r .accessToken)

# 2. List critical assets
curl -s http://localhost:8080/api/v1/assets?criticality=CRITICAL \
  -H "Authorization: Bearer $TOKEN" | jq

# 3. Ask the AI assistant
curl -s -X POST http://localhost:8080/api/v1/rag/ask \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"question":"Which assets have the highest risk score?"}' | jq
```
