from fastapi import APIRouter

from app.models.schemas import AnomalyScoreRequest, AnomalyScoreResponse
from app.services.anomaly_service import MODEL_VERSION, anomaly_detection_service

router = APIRouter(prefix="/api/v1/anomaly", tags=["Anomaly Detection"])


@router.post("/score", response_model=AnomalyScoreResponse, response_model_by_alias=True)
def score_event(request: AnomalyScoreRequest) -> AnomalyScoreResponse:
    result = anomaly_detection_service.score(
        event_type=request.event_type,
        severity=request.severity,
        source_port=request.source_port,
        destination_port=request.destination_port,
        protocol=request.protocol,
        hour_of_day=request.hour_of_day,
        day_of_week=request.day_of_week,
        asset_criticality_weight=request.asset_criticality_weight,
        asset_exposure_weight=request.asset_exposure_weight,
    )
    return AnomalyScoreResponse(
        anomaly_score=result.anomaly_score,
        is_anomaly=result.is_anomaly,
        model_version=MODEL_VERSION,
    )
