from app.services.embedding_service import EMBEDDING_DIMENSIONS, embed_text


def test_embedding_has_expected_dimensionality():
    vector = embed_text("Critical vulnerability CVE-2024-1234 on payment-gateway-01")
    assert len(vector) == EMBEDDING_DIMENSIONS


def test_embedding_is_deterministic():
    text = "Asset payment-gateway-01 has an enterprise risk score of 82.3 (CRITICAL tier)."
    assert embed_text(text) == embed_text(text)


def test_similar_text_is_more_similar_than_unrelated_text():
    import numpy as np

    base = embed_text("Critical vulnerability on payment-gateway-01 with CVSS 9.8")
    similar = embed_text("Critical vulnerability on payment-gateway-02 with CVSS 9.6")
    unrelated = embed_text("Quarterly HR onboarding checklist for new employees")

    def cosine(a, b):
        a, b = np.array(a), np.array(b)
        return float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b) + 1e-9))

    assert cosine(base, similar) > cosine(base, unrelated)
