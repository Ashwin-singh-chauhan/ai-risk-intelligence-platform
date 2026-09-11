from fastapi import APIRouter

from app.models.schemas import EmbeddingRequest, EmbeddingResponse
from app.services.embedding_service import EMBEDDING_DIMENSIONS, embed_text

router = APIRouter(prefix="/api/v1/embeddings", tags=["Embeddings"])


@router.post("", response_model=EmbeddingResponse)
def create_embedding(request: EmbeddingRequest) -> EmbeddingResponse:
    vector = embed_text(request.text)
    return EmbeddingResponse(embedding=vector, dimensions=EMBEDDING_DIMENSIONS)
