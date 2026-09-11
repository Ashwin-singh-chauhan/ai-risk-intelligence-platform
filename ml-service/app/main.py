"""AI-Powered Enterprise Risk Intelligence Platform - ML microservice.

Hosts the Isolation Forest anomaly-detection model consumed by the Spring Boot
backend's security-event ingestion pipeline, and the embedding endpoint used by
the RAG security assistant. See docs/ML_MODEL.md and docs/RAG_FLOW.md.
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.routers import anomaly, embeddings

app = FastAPI(
    title="ERIP ML Service",
    description="Anomaly detection and embedding microservice for the Enterprise Risk Intelligence Platform",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(anomaly.router)
app.include_router(embeddings.router)


@app.get("/health", tags=["Health"])
def health() -> dict:
    return {"status": "UP", "service": "erip-ml-service"}
