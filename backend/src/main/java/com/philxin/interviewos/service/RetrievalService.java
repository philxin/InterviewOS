package com.philxin.interviewos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.common.LogSanitizer;
import com.philxin.interviewos.config.MultiDatabaseProperties;
import com.philxin.interviewos.config.RagProperties;
import com.philxin.interviewos.entity.KnowledgeChunk;
import com.philxin.interviewos.entity.KnowledgeChunkStatus;
import com.philxin.interviewos.entity.KnowledgeDocument;
import com.philxin.interviewos.entity.RetrievalTrace;
import com.philxin.interviewos.repository.KnowledgeChunkRepository;
import com.philxin.interviewos.repository.KnowledgeDocumentRepository;
import com.philxin.interviewos.repository.RetrievalTraceRepository;
import com.pgvector.PGvector;
import java.sql.SQLException;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * V3 最小可用检索服务：embedding + 余弦相似度排序。
 */
@Service
public class RetrievalService {
    private static final Logger log = LoggerFactory.getLogger(RetrievalService.class);

    private final EmbeddingService embeddingService;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final RetrievalTraceRepository retrievalTraceRepository;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate postgresqlJdbcTemplate;
    private final MultiDatabaseProperties multiDatabaseProperties;
    private final RagProperties ragProperties;
    private volatile Boolean pgvectorAvailable;

    public RetrievalService(
        EmbeddingService embeddingService,
        KnowledgeChunkRepository knowledgeChunkRepository,
        KnowledgeDocumentRepository knowledgeDocumentRepository,
        RetrievalTraceRepository retrievalTraceRepository,
        @Qualifier("postgresqlJdbcTemplate") JdbcTemplate postgresqlJdbcTemplate,
        MultiDatabaseProperties multiDatabaseProperties,
        RagProperties ragProperties,
        ObjectMapper objectMapper
    ) {
        this.embeddingService = embeddingService;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.retrievalTraceRepository = retrievalTraceRepository;
        this.postgresqlJdbcTemplate = postgresqlJdbcTemplate;
        this.multiDatabaseProperties = multiDatabaseProperties;
        this.ragProperties = ragProperties;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public RetrievalResult search(AuthenticatedUser authenticatedUser, String query, UUID documentId, Integer requestedTopK) {
        Long userId = getCurrentUserId(authenticatedUser);
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "query must not be blank");
        }
        int topK = resolveTopK(requestedTopK);
        if (documentId != null) {
            knowledgeDocumentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Knowledge document not found"));
        }
        RetrievalResult result;
        if (canUsePgvectorSearch()) {
            try {
                result = searchWithPgvector(normalizedQuery, documentId, topK, userId);
                persistTrace(userId, documentId, result);
                return result;
            } catch (RuntimeException exception) {
                log.warn("Falling back to in-memory retrieval after pgvector query failure", exception);
            }
        }

        List<KnowledgeChunk> candidates = documentId == null
            ? knowledgeChunkRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, KnowledgeChunkStatus.READY)
            : knowledgeChunkRepository.findByUserIdAndDocumentIdAndStatusOrderByChunkIndexAsc(userId, documentId, KnowledgeChunkStatus.READY);
        if (candidates.isEmpty()) {
            result = new RetrievalResult(normalizedQuery, documentId, topK, true, List.of());
            persistTrace(userId, documentId, result);
            return result;
        }
        result = searchInMemory(normalizedQuery, documentId, topK, candidates, userId);
        persistTrace(userId, documentId, result);
        return result;
    }

    private RetrievalResult searchInMemory(
        String normalizedQuery,
        UUID documentId,
        int topK,
        List<KnowledgeChunk> candidates,
        Long userId
    ) {
        float[] queryEmbedding = embeddingService.embed(normalizedQuery);
        double minScore = ragProperties.getMinSimilarityScore() == null ? 0.25d : ragProperties.getMinSimilarityScore();
        List<RetrievalMatch> matches = new ArrayList<>();
        for (KnowledgeChunk candidate : candidates) {
            float[] embedding = readEmbedding(candidate.getEmbedding());
            if (embedding.length == 0) {
                continue;
            }
            double score = cosineSimilarity(queryEmbedding, embedding);
            if (score < minScore) {
                continue;
            }
            matches.add(new RetrievalMatch(candidate, score));
        }
        matches.sort(Comparator.comparingDouble(RetrievalMatch::score).reversed());
        List<RetrievalMatch> topMatches = matches.stream().limit(topK).toList();
        boolean degraded = topMatches.isEmpty();
        log.info(
            "RAG search completed: userId={}, queryLength={}, queryFingerprint={}, topK={}, candidates={}, hits={}, degraded={}",
            userId,
            LogSanitizer.length(normalizedQuery),
            LogSanitizer.fingerprint(normalizedQuery),
            topK,
            candidates.size(),
            topMatches.size(),
            degraded
        );
        return new RetrievalResult(normalizedQuery, documentId, topK, degraded, topMatches);
    }

    private RetrievalResult searchWithPgvector(String normalizedQuery, UUID documentId, int topK, Long userId) {
        float[] queryEmbedding = embeddingService.embed(normalizedQuery);
        String vectorLiteral = writeEmbedding(queryEmbedding);
        int queryEmbeddingDimension = queryEmbedding.length;
        double minScore = ragProperties.getMinSimilarityScore() == null ? 0.25d : ragProperties.getMinSimilarityScore();
        List<RetrievalMatch> matches = postgresqlJdbcTemplate.query(
            """
                WITH query_vec AS (
                    SELECT CAST(? AS vector) AS embedding
                )
                SELECT
                    kc.id,
                    kc.document_id,
                    kd.title AS document_title,
                    kc.text,
                    kc.page_from,
                    kc.page_to,
                    kc.start_offset,
                    kc.end_offset,
                    1 - (CAST(kc.embedding AS vector) <=> qv.embedding) AS score
                FROM knowledge_chunk kc
                JOIN knowledge_document kd ON kd.id = kc.document_id
                CROSS JOIN query_vec qv
                WHERE kc.user_id = ?
                  AND kc.status = 'READY'
                  AND kc.embedding IS NOT NULL
                  AND (
                        kc.embedding_dim = ?
                        OR (kc.embedding_dim IS NULL AND vector_dims(CAST(kc.embedding AS vector)) = ?)
                  )
                  AND (? IS NULL OR kc.document_id = ?)
                  AND (1 - (CAST(kc.embedding AS vector) <=> qv.embedding)) >= ?
                ORDER BY CAST(kc.embedding AS vector) <=> qv.embedding
                LIMIT ?
                """,
            (rs, rowNum) -> {
                KnowledgeDocument document = new KnowledgeDocument();
                document.setId(UUID.fromString(rs.getString("document_id")));
                document.setTitle(rs.getString("document_title"));

                KnowledgeChunk chunk = new KnowledgeChunk();
                chunk.setId(rs.getLong("id"));
                chunk.setDocument(document);
                chunk.setText(rs.getString("text"));
                chunk.setPageFrom((Integer) rs.getObject("page_from"));
                chunk.setPageTo((Integer) rs.getObject("page_to"));
                chunk.setStartOffset((Integer) rs.getObject("start_offset"));
                chunk.setEndOffset((Integer) rs.getObject("end_offset"));
                return new RetrievalMatch(chunk, rs.getDouble("score"));
            },
            vectorLiteral,
            userId,
            queryEmbeddingDimension,
            queryEmbeddingDimension,
            documentId,
            documentId,
            minScore,
            topK
        );
        boolean degraded = matches.isEmpty();
        log.info(
            "RAG pgvector search completed: userId={}, queryLength={}, queryFingerprint={}, topK={}, hits={}, degraded={}",
            userId,
            LogSanitizer.length(normalizedQuery),
            LogSanitizer.fingerprint(normalizedQuery),
            topK,
            matches.size(),
            degraded
        );
        return new RetrievalResult(normalizedQuery, documentId, topK, degraded, matches);
    }

    public String writeEmbedding(float[] embedding) {
        try {
            return new PGvector(embedding).getValue();
        } catch (Exception exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist chunk embedding");
        }
    }

    public float[] readEmbedding(String json) {
        if (json == null || json.isBlank()) {
            return new float[0];
        }
        try {
            return new PGvector(json).toArray();
        } catch (SQLException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read chunk embedding");
        }
    }

    private boolean canUsePgvectorSearch() {
        if (multiDatabaseProperties.getPrimary() != MultiDatabaseProperties.DatabaseType.POSTGRESQL) {
            return false;
        }
        if (pgvectorAvailable != null) {
            return pgvectorAvailable;
        }
        synchronized (this) {
            if (pgvectorAvailable != null) {
                return pgvectorAvailable;
            }
            ConnectionCallback<Boolean> callback = connection -> {
                String productName = connection.getMetaData().getDatabaseProductName();
                if (productName == null || !productName.toLowerCase().contains("postgresql")) {
                    return false;
                }
                try (var statement = connection.createStatement()) {
                    statement.execute("CREATE EXTENSION IF NOT EXISTS vector");
                    return true;
                } catch (SQLException exception) {
                    log.warn("Failed to initialize pgvector extension", exception);
                    return false;
                }
            };
            pgvectorAvailable = Boolean.TRUE.equals(postgresqlJdbcTemplate.execute(callback));
            return pgvectorAvailable;
        }
    }

    private double cosineSimilarity(float[] left, float[] right) {
        if (left.length == 0 || right.length == 0 || left.length != right.length) {
            return -1d;
        }
        double dot = 0d;
        double leftNorm = 0d;
        double rightNorm = 0d;
        for (int i = 0; i < left.length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0d || rightNorm == 0d) {
            return -1d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private Long getCurrentUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authenticatedUser.getId();
    }

    private int resolveTopK(Integer requestedTopK) {
        int fallback = ragProperties.getSearchTopK() == null ? 5 : ragProperties.getSearchTopK();
        if (requestedTopK == null) {
            return fallback;
        }
        return Math.max(1, Math.min(requestedTopK, 10));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void persistTrace(Long userId, UUID documentId, RetrievalResult result) {
        try {
            RetrievalTrace trace = new RetrievalTrace();
            trace.setUserId(userId);
            trace.setDocumentId(documentId);
            trace.setQueryFingerprint(LogSanitizer.fingerprint(result.query()));
            trace.setQueryLength(LogSanitizer.length(result.query()));
            trace.setTopK(result.topK());
            trace.setHitCount(result.matches().size());
            trace.setDegraded(result.degraded());
            trace.setChunkIds(writeJson(
                result.matches().stream()
                    .map(match -> match.chunk().getId())
                    .filter(Objects::nonNull)
                    .toList()
            ));
            trace.setScoreDistribution(buildScoreDistribution(result.matches()));
            retrievalTraceRepository.save(trace);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to persist retrieval trace: userId={}, queryFingerprint={}, topK={}",
                userId,
                LogSanitizer.fingerprint(result.query()),
                result.topK(),
                exception
            );
        }
    }

    private String buildScoreDistribution(List<RetrievalMatch> matches) {
        List<Double> scores = matches.stream().map(RetrievalMatch::score).map(this::roundScore).toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("scores", scores);
        if (scores.isEmpty()) {
            payload.put("min", null);
            payload.put("max", null);
            payload.put("avg", null);
            return writeJson(payload);
        }
        double min = scores.stream().mapToDouble(Double::doubleValue).min().orElse(0d);
        double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0d);
        double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0d);
        payload.put("min", roundScore(min));
        payload.put("max", roundScore(max));
        payload.put("avg", roundScore(avg));
        return writeJson(payload);
    }

    private double roundScore(double score) {
        return Math.round(score * 10000d) / 10000d;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist retrieval trace");
        }
    }

    /**
     * 检索结果集合。
     */
    public record RetrievalResult(String query, UUID documentId, int topK, boolean degraded, List<RetrievalMatch> matches) {
    }

    /**
     * 单条命中结果。
     */
    public record RetrievalMatch(KnowledgeChunk chunk, double score) {
        public KnowledgeDocument document() {
            return chunk.getDocument();
        }
    }
}
