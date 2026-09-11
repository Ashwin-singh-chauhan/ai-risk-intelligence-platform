# Security Design

## Authentication

- **Password storage**: BCrypt with a work factor of 12 (`SecurityConfig.passwordEncoder`).
- **Access tokens**: short-lived JWTs (30 min default, `erip.jwt.access-token-ttl-minutes`),
  HMAC-SHA256 signed, carrying `sub` (email), `role`, and `uid` claims.
- **Refresh tokens**: opaque, cryptographically random 64-byte tokens. Only the SHA-256
  hash is stored (`refresh_tokens.token_hash`); the raw token is returned to the client
  once and never persisted. Refresh tokens are single-use - `AuthService.refresh()`
  revokes the presented token and issues a new one (rotation), limiting the blast radius
  of a leaked refresh token to a single use.
- **Stateless sessions**: `SessionCreationPolicy.STATELESS` - no server-side session
  store, so the API scales horizontally without sticky sessions.

## Authorization (RBAC)

Four roles, enforced with method-level `@PreAuthorize` on controllers (not just route
matching), so authorization survives even if a URL pattern is later refactored:

| Role | Intent |
|---|---|
| `ADMIN` | Full administrative access, including user management and RAG reindexing |
| `SECURITY_ANALYST` | Day-to-day operational write access (assets, vulnerabilities, incidents, remediation, compliance assessment) |
| `EXECUTIVE` | Read access + executive dashboard; cannot mutate operational data |
| `VIEWER` | Read-only across the board |

Unauthenticated requests to protected endpoints return `401` (a custom
`HttpStatusEntryPoint` is configured - Spring Security's un-configured default is `403`,
which conflates "who are you" with "you can't do that"); authenticated-but-unauthorized
requests correctly return `403`.

## Secure configuration

- No secrets are hardcoded: `JWT_SECRET`, database credentials, and `LLM_API_KEY` are
  all environment-variable driven (see `docker-compose.yml` and `application.yml`), with
  non-production fallback defaults clearly named (`change-this-in-production-...`).
- CORS is explicitly allow-listed (`erip.cors.allowed-origins`) rather than `*`.
- CSRF protection is disabled deliberately and safely: the API is stateless/token-based
  (no cookies carrying credentials), which is the standard, documented case where CSRF
  protection is unnecessary.

## Input validation & centralized error handling

Every request DTO uses Jakarta Bean Validation (`@NotBlank`, `@DecimalMin/@Max`, etc.);
`GlobalExceptionHandler` converts validation failures into a structured `fieldErrors` map
rather than leaking a stack trace, and generic exceptions are caught and returned as an
opaque `500` (no internal details exposed to the client).

## Audit logging

`AuditService` is called from every mutating operation across every module (asset
create/update, vulnerability status change, incident lifecycle transitions, compliance
assessments, remediation status updates, user creation/activation, login success/failure,
RAG queries). Audit writes are wrapped in a try/catch that logs-and-continues rather than
failing the primary business transaction - auditability must never become a reason a
legitimate operation fails.

## Demo credentials

The seeded demo accounts (`V3__seed_demo_users.sql`) use a shared, published password
(`Passw0rd!123`) purely for local/demo convenience and are clearly marked **not for
production use** in the migration file itself. A production deployment would remove that
migration (or gate it behind a `dev`/`demo` Spring profile) entirely.

## Known limitations / production hardening backlog

This is a portfolio project, not a certified production system. Before real deployment:

- Add token revocation/blacklisting for access tokens (currently only refresh tokens are
  revocable; a compromised access token remains valid until natural expiry).
- Add rate limiting on `/api/v1/auth/login` to slow brute-force attempts.
- Add MFA for `ADMIN`/`SECURITY_ANALYST` roles.
- Run a dependency vulnerability scan (e.g. OWASP Dependency-Check, `npm audit`,
  `pip-audit`) in CI.
- Terminate TLS at the load balancer (see `docs/AWS_DEPLOYMENT.md`) - local Docker
  Compose intentionally runs plaintext HTTP between services for simplicity.
