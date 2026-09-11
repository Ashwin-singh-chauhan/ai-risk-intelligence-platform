from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_endpoint():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_anomaly_score_endpoint_returns_camel_case_fields():
    response = client.post("/api/v1/anomaly/score", json={
        "eventType": "NORMAL_TRAFFIC", "severity": "INFO", "sourcePort": 51000,
        "destinationPort": 443, "protocol": "HTTPS", "hourOfDay": 10, "dayOfWeek": 2,
        "assetCriticalityWeight": 2, "assetExposureWeight": 1,
    })
    assert response.status_code == 200
    body = response.json()
    assert "anomalyScore" in body
    assert "isAnomaly" in body
    assert "modelVersion" in body


def test_embeddings_endpoint():
    response = client.post("/api/v1/embeddings", json={"text": "test document"})
    assert response.status_code == 200
    body = response.json()
    assert body["dimensions"] == 384
    assert len(body["embedding"]) == 384
