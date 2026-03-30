package com.philxin.interviewos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.knowledge.AcceptKnowledgeConceptRequest;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeConceptResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeChunk;
import com.philxin.interviewos.entity.KnowledgeConcept;
import com.philxin.interviewos.entity.KnowledgeConceptStatus;
import com.philxin.interviewos.entity.KnowledgeDocument;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.entity.KnowledgeTag;
import com.philxin.interviewos.repository.KnowledgeConceptRepository;
import com.philxin.interviewos.repository.KnowledgeDocumentRepository;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class KnowledgeConceptServiceTest {

    @Mock
    private KnowledgeConceptRepository knowledgeConceptRepository;

    @Mock
    private KnowledgeDocumentRepository knowledgeDocumentRepository;

    @Mock
    private KnowledgeRepository knowledgeRepository;

    private KnowledgeConceptService knowledgeConceptService;

    @BeforeEach
    void setUp() {
        knowledgeConceptService = new KnowledgeConceptService(
            knowledgeConceptRepository,
            knowledgeDocumentRepository,
            knowledgeRepository,
            new ObjectMapper()
        );
    }

    @Test
    void listDocumentConceptsShouldReturn404WhenDocumentNotOwned() {
        UUID documentId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
        when(knowledgeDocumentRepository.findByIdAndUserId(documentId, 1L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> knowledgeConceptService.listDocumentConcepts(authenticatedUser(1L), documentId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Knowledge document not found", exception.getMessage());
        verify(knowledgeConceptRepository, never()).findByUserIdAndDocumentIdOrderByConfidenceDescCreatedAtDesc(any(), any());
    }

    @Test
    void acceptConceptShouldCreateKnowledgeAndMarkAccepted() {
        Long conceptId = 101L;
        KnowledgeConcept concept = buildConcept(conceptId, KnowledgeConceptStatus.CANDIDATE);
        when(knowledgeConceptRepository.findByIdAndUserId(conceptId, 1L)).thenReturn(Optional.of(concept));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> {
            Knowledge knowledge = invocation.getArgument(0);
            knowledge.setId(99L);
            return knowledge;
        });
        when(knowledgeConceptRepository.save(any(KnowledgeConcept.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcceptKnowledgeConceptRequest request = new AcceptKnowledgeConceptRequest();
        request.setTags(List.of(" Java ", "backend"));

        KnowledgeConceptResponse response = knowledgeConceptService.acceptConcept(authenticatedUser(1L), conceptId, request);

        assertEquals("ACCEPTED", response.getStatus());
        assertEquals(99L, response.getAcceptedKnowledgeId());
        ArgumentCaptor<Knowledge> knowledgeCaptor = ArgumentCaptor.forClass(Knowledge.class);
        verify(knowledgeRepository).save(knowledgeCaptor.capture());
        Knowledge savedKnowledge = knowledgeCaptor.getValue();
        assertEquals("Redis Core", savedKnowledge.getTitle());
        assertEquals("Redis 核心机制摘要", savedKnowledge.getContent());
        assertEquals(KnowledgeSourceType.FILE_IMPORT, savedKnowledge.getSourceType());
        assertEquals(KnowledgeStatus.ACTIVE, savedKnowledge.getStatus());
        assertEquals(List.of("java", "backend"), savedKnowledge.getTags().stream().map(tag -> tag.getTag()).toList());
    }

    @Test
    void rejectConceptShouldMarkRejected() {
        Long conceptId = 102L;
        KnowledgeConcept concept = buildConcept(conceptId, KnowledgeConceptStatus.CANDIDATE);
        when(knowledgeConceptRepository.findByIdAndUserId(conceptId, 1L)).thenReturn(Optional.of(concept));
        when(knowledgeConceptRepository.save(any(KnowledgeConcept.class))).thenAnswer(invocation -> invocation.getArgument(0));

        KnowledgeConceptResponse response = knowledgeConceptService.rejectConcept(authenticatedUser(1L), conceptId);

        assertEquals("REJECTED", response.getStatus());
    }

    @Test
    void acceptConceptShouldMergeToExistingKnowledgeWhenMergeKnowledgeIdProvided() {
        Long conceptId = 103L;
        Long knowledgeId = 77L;
        KnowledgeConcept concept = buildConcept(conceptId, KnowledgeConceptStatus.CANDIDATE);
        when(knowledgeConceptRepository.findByIdAndUserId(conceptId, 1L)).thenReturn(Optional.of(concept));

        Knowledge existingKnowledge = new Knowledge();
        existingKnowledge.setId(knowledgeId);
        existingKnowledge.setUser(buildUser(1L));
        existingKnowledge.setTitle("Redis Existing");
        existingKnowledge.setContent("已有内容");
        existingKnowledge.setMastery(10);
        existingKnowledge.setSourceType(KnowledgeSourceType.MANUAL);
        existingKnowledge.setStatus(KnowledgeStatus.ACTIVE);
        KnowledgeTag existingTag = new KnowledgeTag();
        existingTag.setKnowledge(existingKnowledge);
        existingTag.setTag("redis");
        existingKnowledge.getTags().add(existingTag);
        when(knowledgeRepository.findByIdAndUserId(knowledgeId, 1L)).thenReturn(Optional.of(existingKnowledge));
        when(knowledgeRepository.save(any(Knowledge.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(knowledgeConceptRepository.save(any(KnowledgeConcept.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AcceptKnowledgeConceptRequest request = new AcceptKnowledgeConceptRequest();
        request.setMergeKnowledgeId(knowledgeId);
        request.setContent("候选补充内容");
        request.setTags(List.of(" cache "));

        KnowledgeConceptResponse response = knowledgeConceptService.acceptConcept(authenticatedUser(1L), conceptId, request);

        assertEquals("ACCEPTED", response.getStatus());
        assertEquals(knowledgeId, response.getAcceptedKnowledgeId());
        assertEquals("已有内容\n\n候选补充内容", existingKnowledge.getContent());
        assertEquals(List.of("redis", "cache"), existingKnowledge.getTags().stream().map(tag -> tag.getTag()).toList());
    }

    @Test
    void createConceptCandidatesShouldPersistCandidateWhenMissing() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        document.setUser(buildUser(1L));
        document.setTitle("Redis Core");
        document.setOriginalFileName("redis-core.md");
        KnowledgeChunk chunk1 = new KnowledgeChunk();
        chunk1.setId(11L);
        chunk1.setText("redis 数据结构与持久化");
        KnowledgeChunk chunk2 = new KnowledgeChunk();
        chunk2.setId(12L);
        chunk2.setText("高可用与主从复制");
        when(knowledgeConceptRepository.existsByUserIdAndDocumentIdAndStatus(1L, document.getId(), KnowledgeConceptStatus.CANDIDATE))
            .thenReturn(false);

        knowledgeConceptService.createConceptCandidates(document, List.of(chunk1, chunk2));

        ArgumentCaptor<KnowledgeConcept> conceptCaptor = ArgumentCaptor.forClass(KnowledgeConcept.class);
        verify(knowledgeConceptRepository).save(conceptCaptor.capture());
        KnowledgeConcept saved = conceptCaptor.getValue();
        assertEquals("Redis Core", saved.getName());
        assertEquals(KnowledgeConceptStatus.CANDIDATE, saved.getStatus());
        assertEquals(BigDecimal.valueOf(0.5100d).setScale(4), saved.getConfidence());
        assertEquals("[11,12]", saved.getSupportingChunkIds());
    }

    private KnowledgeConcept buildConcept(Long conceptId, KnowledgeConceptStatus status) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"));
        document.setUser(buildUser(1L));

        KnowledgeConcept concept = new KnowledgeConcept();
        concept.setId(conceptId);
        concept.setUser(buildUser(1L));
        concept.setDocument(document);
        concept.setName("Redis Core");
        concept.setSummary("Redis 核心机制摘要");
        concept.setAliases("[\"redis-core\"]");
        concept.setSupportingChunkIds("[11,12]");
        concept.setConfidence(BigDecimal.valueOf(0.91d));
        concept.setStatus(status);
        concept.setCreatedAt(LocalDateTime.of(2026, 3, 30, 16, 0, 0));
        concept.setUpdatedAt(LocalDateTime.of(2026, 3, 30, 16, 0, 0));
        return concept;
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
}
