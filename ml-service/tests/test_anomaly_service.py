from app.services.anomaly_service import AnomalyDetectionService


def test_suspicious_off_hours_port_scan_scores_higher_than_normal_business_hours_login():
    service = AnomalyDetectionService(n_baseline_samples=1500)

    suspicious = service.score(
        event_type="PORT_SCAN", severity="HIGH", source_port=4444, destination_port=22,
        protocol="TCP", hour_of_day=3, day_of_week=6,
        asset_criticality_weight=4, asset_exposure_weight=3,
    )
    normal = service.score(
        event_type="LOGIN_SUCCESS", severity="INFO", source_port=51000, destination_port=443,
        protocol="HTTPS", hour_of_day=14, day_of_week=2,
        asset_criticality_weight=2, asset_exposure_weight=1,
    )

    assert suspicious.anomaly_score > normal.anomaly_score
    assert suspicious.is_anomaly is True
    assert normal.is_anomaly is False


def test_anomaly_score_is_bounded_between_zero_and_one():
    service = AnomalyDetectionService(n_baseline_samples=800)
    result = service.score(
        event_type="DATA_EXFILTRATION_ATTEMPT", severity="CRITICAL", source_port=1337,
        destination_port=443, protocol="HTTPS", hour_of_day=2, day_of_week=7,
        asset_criticality_weight=4, asset_exposure_weight=3,
    )
    assert 0.0 <= result.anomaly_score <= 1.0
