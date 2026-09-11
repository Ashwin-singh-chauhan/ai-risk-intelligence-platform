"""Isolation-Forest based anomaly detection for ingested security events.

Design notes (see docs/ML_MODEL.md for the full write-up):
 - Features are hand-engineered from the normalized event fields the Spring Boot
   backend sends (severity, event type, ports, protocol, time-of-day, and the
   asset's criticality/exposure weights) rather than raw text, so the model stays
   small, fast, and fully explainable in terms of "why" a request looked unusual.
 - The forest is trained once at process startup on a synthetically generated
   baseline of "normal" enterprise traffic (see _generate_baseline_traffic).
   This keeps the service self-contained and reproducible without requiring a
   pre-existing labeled dataset - a deliberate, documented trade-off for a
   portfolio/demo deployment. In a production deployment this would instead be
   trained/retrained on a rolling window of the tenant's own historical events.
"""
import hashlib
import random
from dataclasses import dataclass

import numpy as np
from sklearn.ensemble import IsolationForest

MODEL_VERSION = "isolation-forest-v1"

SEVERITY_ORDER = {"INFO": 0, "LOW": 1, "MEDIUM": 2, "HIGH": 3, "CRITICAL": 4}

COMMON_PORTS = [22, 80, 443, 3389, 8080, 8443, 3306, 5432, 25, 53]
COMMON_EVENT_TYPES = [
    "LOGIN_SUCCESS", "FIREWALL_BLOCK", "NORMAL_TRAFFIC", "FILE_ACCESS",
    "CONFIG_CHANGE", "DNS_QUERY", "TLS_HANDSHAKE", "HEALTH_CHECK",
]
COMMON_PROTOCOLS = ["TCP", "UDP", "HTTPS", "HTTP", "SSH"]


def _hash_bucket(value: str | None, buckets: int) -> float:
    if not value:
        return 0.0
    digest = hashlib.sha256(value.encode("utf-8")).hexdigest()
    return (int(digest, 16) % buckets) / buckets


def featurize(
    event_type: str,
    severity: str,
    source_port: int | None,
    destination_port: int | None,
    protocol: str | None,
    hour_of_day: int,
    day_of_week: int,
    asset_criticality_weight: float,
    asset_exposure_weight: float,
) -> np.ndarray:
    severity_score = SEVERITY_ORDER.get(severity.upper(), 2) / 4.0
    event_hash = _hash_bucket(event_type, 997)
    protocol_hash = _hash_bucket(protocol, 97)
    src_port_norm = (source_port or 0) / 65535.0
    dst_port_norm = (destination_port or 0) / 65535.0
    hour_norm = hour_of_day / 24.0
    day_norm = day_of_week / 7.0
    criticality_norm = asset_criticality_weight / 4.0
    exposure_norm = asset_exposure_weight / 3.0
    off_hours = 1.0 if hour_of_day < 6 or hour_of_day > 21 else 0.0

    return np.array([
        severity_score, event_hash, protocol_hash, src_port_norm, dst_port_norm,
        hour_norm, day_norm, criticality_norm, exposure_norm, off_hours,
    ])


@dataclass
class AnomalyResult:
    anomaly_score: float
    is_anomaly: bool


class AnomalyDetectionService:
    def __init__(self, contamination: float = 0.06, n_baseline_samples: int = 4000, random_state: int = 42):
        self._model = IsolationForest(
            n_estimators=200,
            contamination=contamination,
            random_state=random_state,
            n_jobs=-1,
        )
        self._rng = random.Random(random_state)
        baseline = self._generate_baseline_traffic(n_baseline_samples)
        self._model.fit(baseline)
        baseline_scores = -self._model.decision_function(baseline)
        self._score_min = float(np.min(baseline_scores))
        self._score_max = float(np.max(baseline_scores))

    def _generate_baseline_traffic(self, n_samples: int) -> np.ndarray:
        rows = []
        for _ in range(n_samples):
            severity = self._rng.choices(
                ["INFO", "LOW", "MEDIUM", "HIGH"], weights=[0.35, 0.35, 0.25, 0.05]
            )[0]
            event_type = self._rng.choice(COMMON_EVENT_TYPES)
            protocol = self._rng.choice(COMMON_PROTOCOLS)
            src_port = self._rng.randint(1024, 65000)
            dst_port = self._rng.choice(COMMON_PORTS)
            hour = self._weighted_business_hour()
            day = self._rng.randint(1, 5)  # weekday-biased baseline
            criticality = self._rng.uniform(1, 3)
            exposure = self._rng.uniform(1, 2)
            rows.append(featurize(event_type, severity, src_port, dst_port, protocol,
                                   hour, day, criticality, exposure))
        return np.vstack(rows)

    def _weighted_business_hour(self) -> int:
        return int(np.clip(self._rng.gauss(13, 3), 0, 23))

    def score(
        self, event_type: str, severity: str, source_port: int | None, destination_port: int | None,
        protocol: str | None, hour_of_day: int, day_of_week: int,
        asset_criticality_weight: float, asset_exposure_weight: float,
    ) -> AnomalyResult:
        features = featurize(
            event_type, severity, source_port, destination_port, protocol,
            hour_of_day, day_of_week, asset_criticality_weight, asset_exposure_weight,
        ).reshape(1, -1)

        raw_score = float(-self._model.decision_function(features)[0])
        span = max(self._score_max - self._score_min, 1e-6)
        normalized = (raw_score - self._score_min) / span
        normalized = float(np.clip(normalized, 0.0, 1.0))

        is_anomaly = bool(self._model.predict(features)[0] == -1)
        return AnomalyResult(anomaly_score=round(normalized, 5), is_anomaly=is_anomaly)


# Singleton instance created once at import time (module is imported exactly once per process
# by FastAPI's dependency system), so training cost is paid once at service startup.
anomaly_detection_service = AnomalyDetectionService()
