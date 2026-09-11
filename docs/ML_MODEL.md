# ML Anomaly Detection Model

Source: `ml-service/app/services/anomaly_service.py`.

## Algorithm choice: Isolation Forest

Isolation Forest was chosen over supervised alternatives (e.g. a classifier trained on
labeled malicious/benign events) for a reason specific to this domain: **there is no
reliable labeled dataset of "this security event was malicious" available for a
greenfield platform**, and waiting to accumulate one before shipping anomaly detection
defeats the purpose. Isolation Forest is unsupervised - it learns what "normal" looks
like from unlabeled baseline traffic and flags points that are easy to isolate (few
random splits needed) as anomalous. It's also:

- **Fast**: O(n log n) training, sub-millisecond scoring per event - fine for a
  synchronous call in the Kafka consumer's hot path.
- **Robust to irrelevant features**: doesn't require careful feature scaling/selection
  the way distance-based methods (e.g. k-NN, DBSCAN) do.
- **Interpretable at the aggregate level**: contamination rate directly controls the
  expected anomaly rate, which maps to an operational SLA ("flag ~6% of events for
  review") that a security team can reason about.

## Features

Nine hand-engineered features per event (`featurize()` in `anomaly_service.py`), not raw
text - this keeps the model small, fast, and auditable:

| Feature | Source |
|---|---|
| `severity_score` | Event severity, ordinal-encoded 0-1 |
| `event_hash` | SHA-256 hash of event type, bucketed into 997 buckets |
| `protocol_hash` | Same hashing trick for protocol |
| `src_port_norm` / `dst_port_norm` | Source/destination port, normalized 0-1 |
| `hour_norm` / `day_norm` | Time-of-day and day-of-week |
| `criticality_norm` / `exposure_norm` | The affected asset's criticality/exposure weight |
| `off_hours` | Binary flag for outside 06:00-21:00 |

Hashing event type/protocol instead of one-hot encoding a fixed vocabulary means new
event types from a future log source don't require retraining or a schema migration.

## Baseline training data

The model trains once at process startup (`AnomalyDetectionService.__init__`) on 4,000
synthetically generated "normal" events (`_generate_baseline_traffic`): common ports,
common event types (`LOGIN_SUCCESS`, `FIREWALL_BLOCK`, ...), a Gaussian-distributed
business-hours skew, and weekday bias. This is a deliberate, documented trade-off for a
reproducible offline/portfolio deployment - **a production deployment would instead train
on a rolling window of the tenant's own historical events**, so "normal" reflects that
specific environment rather than a generic assumption.

## Score normalization

`IsolationForest.decision_function()` returns higher values for inliers and can be
negative for outliers, which isn't a friendly API contract. The service:

1. Computes `raw_score = -decision_function(x)` (now higher = more anomalous).
2. Min-max normalizes against the *baseline's own* raw-score distribution
   (`_score_min`/`_score_max` captured at training time), clipped to `[0, 1]`.
3. Separately reports `is_anomaly` from `model.predict(x) == -1`, which uses the
   contamination threshold rather than the normalized score - so a consumer can use
   either the boolean flag (operational) or the continuous score (for e.g. sorting a
   dashboard by "most anomalous").

## API contract

`POST /api/v1/anomaly/score` - field names deliberately mirror the Spring Boot backend's
`AnomalyScoreRequest`/`AnomalyScoreResponse` DTOs (camelCase over the wire via Pydantic
aliases) so there's no translation layer between the two services.

## Testing

`ml-service/tests/test_anomaly_service.py` asserts the model scores a clearly suspicious
event (off-hours port scan against a critical, public-facing asset) higher than a clearly
normal one (business-hours HTTPS login), and that scores stay within `[0, 1]`.
