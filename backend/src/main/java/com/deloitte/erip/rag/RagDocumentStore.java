package com.deloitte.erip.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Direct JDBC access to the pgvector-backed rag_documents table. Spring Data JPA does not yet
 * offer first-class mapping for the Postgres `vector` type, so similarity search and embedding
 * writes go through JdbcTemplate with explicit `::vector` casts rather than the JPA repositories
 * used elsewhere in the codebase - a deliberate, documented exception (see docs/RAG_FLOW.md).
 */
@Repository
@RequiredArgsConstructor
public class RagDocumentStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void upsert(String sourceType, String sourceId, String title, String content,
                        double[] embedding, Map<String, Object> metadata) {
        String vectorLiteral = toVectorLiteral(embedding);
        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (Exception e) {
            metadataJson = "{}";
        }

        Integer existing = jdbcTemplate.query(
                "SELECT 1 FROM rag_documents WHERE source_type = ? AND source_id = ?::uuid LIMIT 1",
                rs -> rs.next() ? 1 : null, sourceType, sourceId);

        if (existing != null) {
            jdbcTemplate.update("""
                    UPDATE rag_documents SET title = ?, content = ?, embedding = ?::vector,
                        metadata = ?::jsonb, updated_at = now()
                    WHERE source_type = ? AND source_id = ?::uuid
                    """, title, content, vectorLiteral, metadataJson, sourceType, sourceId);
        } else {
            jdbcTemplate.update("""
                    INSERT INTO rag_documents (source_type, source_id, title, content, embedding, metadata)
                    VALUES (?, ?::uuid, ?, ?, ?::vector, ?::jsonb)
                    """, sourceType, sourceId, title, content, vectorLiteral, metadataJson);
        }
    }

    public List<RagDocumentMatch> findSimilar(double[] queryEmbedding, int limit) {
        String vectorLiteral = toVectorLiteral(queryEmbedding);
        return jdbcTemplate.query("""
                SELECT id, source_type, source_id, title, content,
                       1 - (embedding <=> ?::vector) AS similarity
                FROM rag_documents
                ORDER BY embedding <=> ?::vector
                LIMIT ?
                """,
                (rs, rowNum) -> new RagDocumentMatch(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("source_type"),
                        rs.getString("source_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        rs.getDouble("similarity")),
                vectorLiteral, vectorLiteral, limit);
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rag_documents", Long.class);
        return count == null ? 0 : count;
    }

    private String toVectorLiteral(double[] embedding) {
        String joined = IntStream.range(0, embedding.length)
                .mapToObj(i -> String.valueOf(embedding[i]))
                .collect(Collectors.joining(","));
        return "[" + joined + "]";
    }
}
