"""Deterministic, dependency-free text embeddings for the RAG assistant.

Trade-off (documented in docs/RAG_FLOW.md): a real deployment would call a hosted
embedding model (OpenAI text-embedding-3-small, Cohere, or a local sentence-transformers
model). Those require either an API key/network egress or a multi-hundred-MB model
download at build time. For a reproducible, fully offline portfolio deployment this
service instead uses scikit-learn's HashingVectorizer, which deterministically maps
text to a fixed-length dense vector via feature hashing - no training, no downloads,
same output on every machine. It captures lexical (bag-of-words) similarity well
enough to ground the assistant's retrieval over the platform's short, templated
records; it does not capture deep semantic similarity the way a transformer would.
"""
from sklearn.feature_extraction.text import HashingVectorizer

EMBEDDING_DIMENSIONS = 384

_vectorizer = HashingVectorizer(
    n_features=EMBEDDING_DIMENSIONS,
    alternate_sign=False,
    norm="l2",
    ngram_range=(1, 2),
)


def embed_text(text: str) -> list[float]:
    vector = _vectorizer.transform([text]).toarray()[0]
    return vector.tolist()
