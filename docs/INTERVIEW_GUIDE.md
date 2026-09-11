# Interview Guide: Architectural Decisions

This document explains the *actual* decisions made building this platform - what was
chosen, what alternatives were considered, and why - so they can be defended in an
interview rather than just recited. Cross-references point to the deeper technical docs.

## "Walk me through the architecture at a high level."

Three deployable services: a Spring Boot API that owns all business logic and RBAC, a
Python FastAPI sidecar for the two ML-shaped problems (anomaly detection, embeddings),
and a React SPA. See [ARCHITECTURE.md](ARCHITECTURE.md) for the component diagram.

**Why not full microservices** (separate services per domain - assets, vulnerabilities,
incidents, etc.)? At this scale, splitting `asset`/`vulnerability`/`incident`/`risk` into
independently deployed services would multiply operational overhead (service discovery,
distributed transactions across what are fundamentally one team's tightly-coupled
read/write patterns - the risk engine reads from four other modules on every
computation) without a corresponding scaling benefit. The package-per-domain structure
inside one Spring Boot app (see the module list in ARCHITECTURE.md) gets most of
microservices' organizational benefit - clear ownership boundaries, one-directional
dependencies - without the distributed-systems tax. The one thing that *is* split out
(the ML service) is split because it needs a fundamentally different runtime
(Python/scikit-learn) - a technical reason, not an organizational one.

## "Why is the risk score a hand-written formula instead of a trained model?"

See [RISK_ENGINE.md](RISK_ENGINE.md) for the full reasoning. Short version: a risk score
that drives remediation prioritization and executive reporting has to be explainable on
demand ("why is this asset rated 87?") in a way a gradient-boosted model's SHAP values
approximate but a hand-written weighted formula answers exactly and cheaply. The
trade-off is real - a learned model could plausibly fit historical outcomes better - but
"plausibly better fit" isn't worth losing the ability to answer that question in one
database read with zero additional infrastructure (no SHAP/LIME service, no model
versioning for the risk score itself).

## "Where does ML actually get used, then?"

Anomaly detection on security events (Isolation Forest) - a problem that's the *opposite*
shape from risk scoring. There's no ground truth for "was this specific network event
malicious," explainability per-event matters less than catching the pattern, and
unsupervised outlier detection is the right tool. See [ML_MODEL.md](ML_MODEL.md) for the
feature engineering and the documented trade-off of training on synthetic baseline
traffic rather than requiring a pre-existing labeled dataset.

## "Why Kafka for event ingestion instead of a direct API call to Postgres?"

Throughput decoupling: a burst of security events shouldn't be gated by database write
latency, and the event source (a SIEM/EDR in a real deployment) shouldn't need to know
or care whether the consumer is up. See [KAFKA_PIPELINE.md](KAFKA_PIPELINE.md) for the
full flow, including the dead-letter-queue error path and why ML scoring is designed to
fail soft (an unavailable ML service degrades to "ungraded" events, not a stalled
pipeline).

## "How does the RAG assistant avoid hallucinating asset names and numbers?"

Two mechanisms, both visible in [RAG_FLOW.md](RAG_FLOW.md): (1) the system prompt
explicitly constrains the model to answer only from the retrieved `CONTEXT` block and to
say so when it can't, and (2) every response returns the actual retrieved documents
(`referencedDocuments`) so the frontend - and the user - can verify the grounding rather
than trust the model's word. The knowledge base itself is built from live database rows
(assets, vulnerabilities, incidents, risk scores), re-indexed every 30 minutes, so answers
don't drift stale the way a one-time static corpus would.

## "Why HashingVectorizer instead of a real embedding model, and why does that matter?"

Documented explicitly in [RAG_FLOW.md](RAG_FLOW.md) as a trade-off, not hidden: a hosted
embedding API needs a key and network egress; a local transformer model needs a
multi-hundred-MB download. Both add friction disproportionate to a project meant to run
identically via `docker compose up` on any machine. HashingVectorizer is deterministic
and dependency-free but only captures lexical similarity, not deep semantics - a limitation
worth naming precisely because the *fix* (swap the embedding backend) is a contained
change thanks to the same interface-based design that makes swapping the LLM provider
(`erip.llm.provider=openai`) a one-line config change.

## "Why JDBC/JdbcTemplate for one table (`rag_documents`) instead of JPA like everything else?"

Because Hibernate as bundled with Spring Boot 3.3 has no first-class mapping for
Postgres's `vector` type or its `<=>` distance operator. Rather than fight the ORM with a
custom Hibernate `UserType`, `RagDocumentStore` uses `JdbcTemplate` with explicit
`::vector` casts for that one table. This is called out explicitly in code comments and
[RAG_FLOW.md](RAG_FLOW.md) as a narrow, deliberate exception - the kind of pragmatic call
worth being able to explain rather than defend as "the right way to do everything."

## "How is remediation prioritization decoupled from vulnerability management?"

`VulnerabilityService` publishes a `VulnerabilityCreatedEvent` via Spring's
`ApplicationEventPublisher`; `RemediationService` listens via
`@TransactionalEventListener(phase = AFTER_COMMIT)` and generates a recommendation only
after the vulnerability row is durably committed. This means the vulnerability module has
zero compile-time dependency on remediation - it doesn't know remediation exists - which
keeps the dependency graph one-directional (see the module list in ARCHITECTURE.md) and
means a future third listener (e.g. auto-filing a ticket in an external system) could be
added without touching `VulnerabilityService` at all.

## "What would you change for a real multi-tenant SaaS version of this?"

- Add a `tenant_id` column (or schema-per-tenant) - currently single-tenant by design,
  which is the honest state of the code today.
- Move the anomaly-detection baseline from synthetic data to a rolling window of each
  tenant's own historical events (flagged as the production path in ML_MODEL.md).
- Add token revocation for access tokens, not just refresh tokens (flagged in SECURITY.md).
- Introduce the AWS deployment topology in [AWS_DEPLOYMENT.md](AWS_DEPLOYMENT.md) with
  per-tenant data isolation considerations for RDS and MSK.

## "How did you verify this actually works, not just that it compiles?"

- Backend: `RiskEngineTest`/`RemediationEngineTest` are pure unit tests over the scoring
  logic (no Spring context, no DB) - 14 assertions covering monotonicity properties
  (e.g. an exploited+unpatched vulnerability must score higher than a plain one) rather
  than just "doesn't throw." `JwtServiceTest` covers token issuance/validation.
  `AssetApiIntegrationTest` uses Testcontainers to exercise the full HTTP -> Spring
  Security -> JPA -> real Postgres path, including asserting `401` vs `403` are returned
  in the right cases.
- ML service: `pytest` asserts the Isolation Forest actually ranks a synthetic port-scan
  above a synthetic normal login (not just "returns a number"), and that the embedding
  service produces closer vectors for related text than unrelated text.
- Frontend: Vitest + React Testing Library cover the auth store's state transitions and
  the login form's success/failure/demo-account-fill paths.
- Manual verification: the ML service's anomaly and embedding endpoints were smoke-tested
  in-process; the frontend was rendered in a real browser to confirm the Tailwind theme
  and login flow work end-to-end, not just that `tsc` was happy.
