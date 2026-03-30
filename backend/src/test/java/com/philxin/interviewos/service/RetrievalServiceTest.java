package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.config.MultiDatabaseProperties;
import com.philxin.interviewos.config.RagProperties;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.KnowledgeChunk;
import com.philxin.interviewos.entity.KnowledgeChunkStatus;
import com.philxin.interviewos.entity.KnowledgeDocument;
import com.philxin.interviewos.entity.KnowledgeDocumentStatus;
import com.philxin.interviewos.entity.RetrievalTrace;
import com.philxin.interviewos.repository.KnowledgeChunkRepository;
import com.philxin.interviewos.repository.KnowledgeDocumentRepository;
import com.philxin.interviewos.repository.RetrievalTraceRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class RetrievalServiceTest {

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private RetrievalTraceRepository retrievalTraceRepository;

    @Mock
    private JdbcTemplate postgresqlJdbcTemplate;

    private RetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        MultiDatabaseProperties properties = new MultiDatabaseProperties();
        properties.setPrimary(MultiDatabaseProperties.DatabaseType.MYSQL);

        RagProperties ragProperties = new RagProperties();
        ragProperties.setSearchTopK(5);
        ragProperties.setMinSimilarityScore(0.25d);

        retrievalService = new RetrievalService(
            embeddingService,
            knowledgeChunkRepository,
            knowledgeDocumentRepository,
            retrievalTraceRepository,
            postgresqlJdbcTemplate,
            properties,
            ragProperties,
            new ObjectMapper()
        );
    }

    @Test
    void searchShouldNotCrossUserResultsAndShouldPersistTrace() {
        when(embeddingService.embed(anyString())).thenReturn(new float[] {1f, 0f});
        when(knowledgeChunkRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(1L, KnowledgeChunkStatus.READY))
            .thenReturn(List.of(buildChunk(101L, "[1,0]", "user-1-doc")));
        when(knowledgeChunkRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(2L, KnowledgeChunkStatus.READY))
            .thenReturn(List.of(buildChunk(202L, "[1,0]", "user-2-doc")));
        when(retrievalTraceRepository.save(any(RetrievalTrace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RetrievalService.RetrievalResult userOneResult = retrievalService.search(authenticatedUser(1L), "redis", null, 5);
        RetrievalService.RetrievalResult userTwoResult = retrievalService.search(authenticatedUser(2L), "redis", null, 5);

        assertEquals(1, userOneResult.matches().size());
        assertEquals(1, userTwoResult.matches().size());
        assertEquals(101L, userOneResult.matches().get(0).chunk().getId());
        assertEquals(202L, userTwoResult.matches().get(0).chunk().getId());
        verify(knowledgeChunkRepository).findByUserIdAndStatusOrderByUpdatedAtDesc(1L, KnowledgeChunkStatus.READY);
        verify(knowledgeChunkRepository).findByUserIdAndStatusOrderByUpdatedAtDesc(2L, KnowledgeChunkStatus.READY);

        ArgumentCaptor<RetrievalTrace> captor = ArgumentCaptor.forClass(RetrievalTrace.class);
        verify(retrievalTraceRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<RetrievalTrace> traces = captor.getAllValues();
        assertEquals(1L, traces.get(0).getUserId());
        assertEquals(2L, traces.get(1).getUserId());
        assertFalse(traces.get(0).isDegraded());
        assertTrue(traces.get(0).getChunkIds().contains("101"));
        assertTrue(traces.get(0).getScoreDistribution().contains("\"scores\""));
    }

    @Test
    void searchShouldPersistDegradedTraceWhenNoCandidateChunkExists() {
        when(knowledgeChunkRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(1L, KnowledgeChunkStatus.READY))
            .thenReturn(List.of());
        when(retrievalTraceRepository.save(any(RetrievalTrace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RetrievalService.RetrievalResult result = retrievalService.search(authenticatedUser(1L), "spring", null, 5);

        assertTrue(result.degraded());
        assertEquals(0, result.matches().size());
        ArgumentCaptor<RetrievalTrace> captor = ArgumentCaptor.forClass(RetrievalTrace.class);
        verify(retrievalTraceRepository).save(captor.capture());
        RetrievalTrace trace = captor.getValue();
        assertEquals(1L, trace.getUserId());
        assertEquals(5, trace.getTopK());
        assertEquals(0, trace.getHitCount());
        assertTrue(trace.isDegraded());
        assertEquals("[]", trace.getChunkIds());
    }

    @Test
    void searchShouldReturn404WhenDocumentIsNotOwnedByCurrentUser() {
        UUID documentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(knowledgeDocumentRepository.findByIdAndUserId(documentId, 1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> retrievalService.search(authenticatedUser(1L), "java", documentId, 5)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(retrievalTraceRepository, never()).save(any(RetrievalTrace.class));
    }

    private AuthenticatedUser authenticatedUser(Long id) {
        return AuthenticatedUser.fromEntity(buildUser(id));
    }

    private AppUser buildUser(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setDisplayName("philxin");
        user.setPasswordHash("hash");
        return user;
    }

    private KnowledgeChunk buildChunk(Long chunkId, String embedding, String documentTitle) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(UUID.randomUUID());
        document.setTitle(documentTitle);
        document.setStatus(KnowledgeDocumentStatus.ACTIVE);

        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setId(chunkId);
        chunk.setDocument(document);
        chunk.setStatus(KnowledgeChunkStatus.READY);
        chunk.setText("chunk content");
        chunk.setEmbedding(embedding);
        return chunk;
    }
}
