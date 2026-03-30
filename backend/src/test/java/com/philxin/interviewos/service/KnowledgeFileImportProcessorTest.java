package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeChunk;
import com.philxin.interviewos.entity.KnowledgeChunkStatus;
import com.philxin.interviewos.entity.KnowledgeDocument;
import com.philxin.interviewos.entity.KnowledgeDocumentStatus;
import com.philxin.interviewos.entity.KnowledgeFileImport;
import com.philxin.interviewos.entity.KnowledgeFileImportStatus;
import com.philxin.interviewos.repository.KnowledgeChunkRepository;
import com.philxin.interviewos.repository.KnowledgeDocumentRepository;
import com.philxin.interviewos.repository.KnowledgeFileImportRepository;
import com.philxin.interviewos.repository.KnowledgeRepository;
import java.nio.charset.StandardCharsets;
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

@ExtendWith(MockitoExtension.class)
class KnowledgeFileImportProcessorTest {

    @Mock
    private KnowledgeFileImportRepository knowledgeFileImportRepository;

    @Mock
    private KnowledgeRepository knowledgeRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Mock
    private KnowledgeFileImportParser parser;

    @Mock
    private KnowledgeChunkingService knowledgeChunkingService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private RetrievalService retrievalService;

    @Mock
    private KnowledgeConceptService knowledgeConceptService;

    private KnowledgeFileImportProcessor knowledgeFileImportProcessor;

    @BeforeEach
    void setUp() {
        knowledgeFileImportProcessor = new KnowledgeFileImportProcessor(
            knowledgeFileImportRepository,
            knowledgeRepository,
            knowledgeDocumentRepository,
            knowledgeChunkRepository,
            parser,
            knowledgeChunkingService,
            embeddingService,
            retrievalService,
            knowledgeConceptService,
            new ObjectMapper()
        );
    }

    @Test
    void processImportShouldNotExposeInternalStackInFailureReason() {
        UUID importId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        KnowledgeFileImport fileImport = new KnowledgeFileImport();
        fileImport.setId(importId);
        fileImport.setUser(buildUser(1L));
        fileImport.setFileName("java-notes.pdf");
        fileImport.setStatus(KnowledgeFileImportStatus.PENDING);
        when(knowledgeFileImportRepository.findById(importId)).thenReturn(Optional.of(fileImport));
        when(knowledgeFileImportRepository.save(any(KnowledgeFileImport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parser.extractText(any(), any())).thenThrow(
            new RuntimeException("Parser failed\nat com.internal.Parser.parse(Parser.java:32)\nCaused by: java.lang.IllegalStateException")
        );

        knowledgeFileImportProcessor.processImport(importId, "pdf".getBytes(StandardCharsets.UTF_8), List.of("java"));

        ArgumentCaptor<KnowledgeFileImport> captor = ArgumentCaptor.forClass(KnowledgeFileImport.class);
        verify(knowledgeFileImportRepository, atLeastOnce()).save(captor.capture());
        List<KnowledgeFileImport> allSaved = captor.getAllValues();
        KnowledgeFileImport finalSaved = allSaved.get(allSaved.size() - 1);

        assertEquals(KnowledgeFileImportStatus.FAILED, finalSaved.getStatus());
        assertEquals("File import failed due to parser or indexing error", finalSaved.getFailureReason());
        assertFalse(finalSaved.getFailureReason().contains("Parser.java:32"));
    }

    @Test
    void processImportShouldSkipWhenAlreadyCompleted() {
        UUID importId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
        KnowledgeFileImport fileImport = new KnowledgeFileImport();
        fileImport.setId(importId);
        fileImport.setUser(buildUser(1L));
        fileImport.setFileName("done.pdf");
        fileImport.setStatus(KnowledgeFileImportStatus.SUCCESS);
        when(knowledgeFileImportRepository.findById(importId)).thenReturn(Optional.of(fileImport));

        knowledgeFileImportProcessor.processImport(importId, "pdf".getBytes(StandardCharsets.UTF_8), List.of());

        verify(knowledgeFileImportRepository, never()).save(any(KnowledgeFileImport.class));
        verify(parser, never()).extractText(any(), any());
        verify(knowledgeChunkRepository, never()).saveAll(any());
    }

    @Test
    void processImportShouldKeepReadableReasonWhenDocumentExceedsChunkLimit() {
        UUID importId = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
        KnowledgeFileImport fileImport = new KnowledgeFileImport();
        fileImport.setId(importId);
        fileImport.setUser(buildUser(1L));
        fileImport.setFileName("huge.md");
        fileImport.setContentType("text/markdown");
        fileImport.setFileSize(1024L);
        fileImport.setStatus(KnowledgeFileImportStatus.PENDING);
        when(knowledgeFileImportRepository.findById(importId)).thenReturn(Optional.of(fileImport));
        when(knowledgeFileImportRepository.save(any(KnowledgeFileImport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parser.extractText(anyString(), any(byte[].class))).thenReturn("long content");
        when(knowledgeChunkingService.normalize(anyString())).thenReturn("long content");
        when(knowledgeChunkingService.getChunkStrategy()).thenReturn("char-window-overlap-v1");
        when(knowledgeChunkingService.split(anyString()))
            .thenThrow(new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Document exceeds max chunk limit"));
        when(embeddingService.getModel()).thenReturn("text-embedding-3-large");
        when(embeddingService.getDimensions()).thenReturn(3072);
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        knowledgeFileImportProcessor.processImport(importId, "huge".getBytes(StandardCharsets.UTF_8), List.of("java"));

        ArgumentCaptor<KnowledgeFileImport> captor = ArgumentCaptor.forClass(KnowledgeFileImport.class);
        verify(knowledgeFileImportRepository, atLeastOnce()).save(captor.capture());
        List<KnowledgeFileImport> allSaved = captor.getAllValues();
        KnowledgeFileImport finalSaved = allSaved.get(allSaved.size() - 1);

        assertEquals(KnowledgeFileImportStatus.FAILED, finalSaved.getStatus());
        assertEquals("Document exceeds max chunk limit", finalSaved.getFailureReason());
        assertFalse(finalSaved.getFailureReason().contains("com.philxin"));
        verify(knowledgeChunkRepository, never()).saveAll(any());
    }

    @Test
    void processImportShouldMarkPartialWhenSomeChunkEmbeddingFails() {
        UUID importId = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
        KnowledgeFileImport fileImport = new KnowledgeFileImport();
        fileImport.setId(importId);
        fileImport.setUser(buildUser(1L));
        fileImport.setFileName("notes.md");
        fileImport.setContentType("text/markdown");
        fileImport.setFileSize(1024L);
        fileImport.setStatus(KnowledgeFileImportStatus.PENDING);
        when(knowledgeFileImportRepository.findById(importId)).thenReturn(Optional.of(fileImport));
        when(knowledgeFileImportRepository.save(any(KnowledgeFileImport.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(parser.extractText(anyString(), any(byte[].class))).thenReturn("chunk one chunk two");
        when(knowledgeChunkingService.normalize(anyString())).thenReturn("chunk one chunk two");
        when(knowledgeChunkingService.getChunkStrategy()).thenReturn("char-window-overlap-v1");
        when(knowledgeChunkingService.split(anyString())).thenReturn(
            List.of(
                new KnowledgeChunkingService.ChunkDraft(0, "chunk one", 0, 9, 2),
                new KnowledgeChunkingService.ChunkDraft(1, "chunk two", 10, 19, 2)
            )
        );
        when(embeddingService.getModel()).thenReturn("text-embedding-3-large");
        when(embeddingService.getDimensions()).thenReturn(3072);
        when(embeddingService.embed(anyString())).thenAnswer(invocation -> {
            String text = invocation.getArgument(0);
            if ("chunk one".equals(text)) {
                return new float[] {0.1f, 0.2f};
            }
            throw new RuntimeException("embedding timeout");
        });
        when(retrievalService.writeEmbedding(any(float[].class))).thenReturn("[0.1,0.2]");
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeDocumentRepository.save(any(KnowledgeDocument.class))).thenAnswer(invocation -> {
            KnowledgeDocument document = invocation.getArgument(0);
            if (document.getId() == null) {
                document.setId(UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee"));
            }
            return document;
        });

        knowledgeFileImportProcessor.processImport(importId, "notes".getBytes(StandardCharsets.UTF_8), List.of("java"));

        ArgumentCaptor<KnowledgeFileImport> importCaptor = ArgumentCaptor.forClass(KnowledgeFileImport.class);
        verify(knowledgeFileImportRepository, atLeastOnce()).save(importCaptor.capture());
        List<KnowledgeFileImport> allImports = importCaptor.getAllValues();
        KnowledgeFileImport finalImport = allImports.get(allImports.size() - 1);
        assertEquals(KnowledgeFileImportStatus.PARTIAL, finalImport.getStatus());
        assertEquals(1, finalImport.getEmbeddedChunks());
        assertEquals(1, finalImport.getFailedChunks());
        assertEquals(1, finalImport.getCreatedCount());

        ArgumentCaptor<KnowledgeDocument> documentCaptor = ArgumentCaptor.forClass(KnowledgeDocument.class);
        verify(knowledgeDocumentRepository, atLeastOnce()).save(documentCaptor.capture());
        List<KnowledgeDocument> allDocuments = documentCaptor.getAllValues();
        KnowledgeDocument finalDocument = allDocuments.get(allDocuments.size() - 1);
        assertEquals(KnowledgeDocumentStatus.ACTIVE, finalDocument.getStatus());
        assertEquals(1, finalDocument.getActiveChunks());

        ArgumentCaptor<KnowledgeChunk> chunkCaptor = ArgumentCaptor.forClass(KnowledgeChunk.class);
        verify(knowledgeChunkRepository, atLeast(2)).save(chunkCaptor.capture());
        List<KnowledgeChunk> persistedChunks = chunkCaptor.getAllValues();
        assertTrue(persistedChunks.stream().anyMatch(chunk -> chunk.getStatus() == KnowledgeChunkStatus.READY));
        assertTrue(persistedChunks.stream().anyMatch(chunk -> chunk.getStatus() == KnowledgeChunkStatus.FAILED));
        assertTrue(
            persistedChunks.stream()
                .anyMatch(chunk -> chunk.getStatus() == KnowledgeChunkStatus.FAILED && "embedding timeout".equals(chunk.getFailureReason()))
        );
    }

    private AppUser buildUser(Long id) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setEmail("user@example.com");
        user.setDisplayName("user");
        user.setPasswordHash("hash");
        return user;
    }
}
