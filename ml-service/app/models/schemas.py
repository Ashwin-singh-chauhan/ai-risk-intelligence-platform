"""Pydantic request/response contracts shared with the Spring Boot backend.

Field names intentionally mirror the Java DTOs in
backend/src/main/java/com/deloitte/erip/anomaly/dto and
backend/src/main/java/com/deloitte/erip/rag/EmbeddingClient.java
so the JSON wire format lines up on both sides without a translation layer.
"""
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class AnomalyScoreRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True, protected_namespaces=())

    event_type: str = Field(alias="eventType")
    severity: str
    source_port: Optional[int] = Field(default=None, alias="sourcePort")
    destination_port: Optional[int] = Field(default=None, alias="destinationPort")
    protocol: Optional[str] = None
    hour_of_day: int = Field(alias="hourOfDay")
    day_of_week: int = Field(alias="dayOfWeek")
    asset_criticality_weight: float = Field(alias="assetCriticalityWeight")
    asset_exposure_weight: float = Field(alias="assetExposureWeight")


class AnomalyScoreResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True, protected_namespaces=())

    anomaly_score: float = Field(serialization_alias="anomalyScore")
    is_anomaly: bool = Field(serialization_alias="isAnomaly")
    model_version: str = Field(serialization_alias="modelVersion")


class EmbeddingRequest(BaseModel):
    text: str


class EmbeddingResponse(BaseModel):
    embedding: list[float]
    dimensions: int


class BatchTrainRequest(BaseModel):
    """Optional endpoint used by the synthetic-data generator to retrain the
    Isolation Forest on freshly generated baseline traffic instead of the
    bundled default baseline."""
    samples: list[AnomalyScoreRequest]
