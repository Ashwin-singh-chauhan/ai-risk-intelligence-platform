# Explainable Risk Engine

Source: `backend/src/main/java/com/deloitte/erip/risk/RiskEngine.java` (pure, unit-tested
scoring logic) and `RiskScoreService.java` (data orchestration + persistence).

## Why rule-based and explainable, not a black-box ML model

A risk score that drives remediation prioritization and executive reporting has to be
defensible in an audit or a board meeting. A gradient-boosted or neural model could
likely produce a marginally "better" score against some historical loss function, but
"the model said so" is not an acceptable answer to "why is this vendor's payment gateway
rated 87/100?" The engine here is a weighted, documented formula precisely so every score
decomposes back into five named components an analyst can inspect, argue with, and
retune. (The ML model in this platform is deliberately scoped to anomaly *detection*,
where explainability matters less and pattern-finding matters more - see
[docs/ML_MODEL.md](ML_MODEL.md).)

## The formula

```
overall_score = 30% * vulnerability_component
              + 20% * exposure_component
              + 20% * threat_activity_component
              + 15% * compliance_component
              + 15% * incident_history_component
```

Each component is independently normalized to 0-100 before weighting, so the weights
above are literally the percentage each factor contributes to the final number.

| Component | Weight | Inputs | Intuition |
|---|---|---|---|
| **Vulnerability** | 30% | Open vulnerabilities' CVSS scores, exploit availability (×1.5), missing patch (×1.2) | The single biggest driver: what's actually broken on this asset right now |
| **Exposure** | 20% | `exposure.weight() * 20 + criticality.weight() * 15` | A critical, internet-facing asset starts "riskier" even with zero known vulnerabilities |
| **Threat activity** | 20% | Security events in the last 30 days, severity-weighted, anomaly flag ×1.5, linear time-decay (recencyFactor floor 0.2) | Recent suspicious activity matters more than an event from three weeks ago |
| **Compliance** | 15% | `100 - compliance_percentage` for the asset's business unit, averaged across NIST CSF/ISO 27001/SOC 2 | Compliance gaps are a *risk multiplier* context, not a primary signal |
| **Incident history** | 15% | Incidents opened in the last 180 days, severity-weighted, linear time-decay (floor 0.15) | A history of incidents on this asset raises the prior for "something will go wrong again" |

Every component is capped at 100 (`RiskEngine.cap()`) so a single catastrophic input
can't blow the final score past what the weighting formula allows.

## Risk tiers

```
score >= 80  -> CRITICAL
score >= 60  -> HIGH
score >= 35  -> MEDIUM
score <  35  -> LOW
```

## The explanation payload

Every `risk_scores` row stores a JSON `explanation` alongside the numeric components:

```json
{
  "weights": { "vulnerability": 0.30, "exposure": 0.20, "threatActivity": 0.20, "compliance": 0.15, "incidentHistory": 0.15 },
  "inputs": { "openVulnerabilityCount": 4, "criticalOpenVulnerabilityCount": 1, "recentEventCount30d": 12, ... },
  "topContributingVulnerabilities": [ { "cveId": "CVE-2024-10042", "cvssScore": 9.8, "exploitAvailable": true } ],
  "narrative": "payment-gateway-01 carries risk primarily driven by open vulnerabilities. Vulnerability=78.4, Exposure=60.0, ..."
}
```

This is what the frontend renders directly in the "Risk Score Explanation" panel on the
Assets page - no client-side recomputation, no separate "explain" endpoint.

## When scores are (re)computed

- **Scheduled**: `RiskScoreService.recomputeAll()` runs every 15 minutes
  (`erip.risk.recompute-interval-ms`), recalculating every active asset.
- **On demand**: `POST /api/v1/risk-scores/assets/{id}/recompute` (ADMIN/SECURITY_ANALYST)
  for an immediate score after e.g. patching a critical vulnerability.
- Because every computation inserts a new row rather than updating in place, the full
  history is queryable via `GET /api/v1/risk-scores/assets/{id}/history` and used to
  drive trend charts without any separate time-series store.

## Tuning

The weights and per-component formulas are constants in `RiskEngine.java`
(`WEIGHT_VULNERABILITY`, etc.) rather than database configuration, by design: changing
risk methodology is a code change that goes through review and the unit test suite in
`RiskEngineTest.java`, not a runtime configuration toggle that could silently drift from
what's documented here.
