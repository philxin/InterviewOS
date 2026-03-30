package com.philxin.interviewos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.knowledge.AcceptKnowledgeConceptRequest;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeConceptResponse;
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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 候选知识点服务：候选查询、接受/拒绝与最小自动抽取落库。
 */
@Service
public class KnowledgeConceptService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeConceptService.class);
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {
    };

    private final KnowledgeConceptRepository knowledgeConceptRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final ObjectMapper objectMapper;

    public KnowledgeConceptService(
        KnowledgeConceptRepository knowledgeConceptRepository,
        KnowledgeDocumentRepository knowledgeDocumentRepository,
        KnowledgeRepository knowledgeRepository,
        ObjectMapper objectMapper
    ) {
        this.knowledgeConceptRepository = knowledgeConceptRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询指定文档下的候选知识点（仅当前用户可见）。
     */
    @Transactional(readOnly = true)
    public List<KnowledgeConceptResponse> listDocumentConcepts(AuthenticatedUser authenticatedUser, UUID documentId) {
        Long userId = getCurrentUserId(authenticatedUser);
        ensureDocumentOwned(userId, documentId);
        return knowledgeConceptRepository.findByUserIdAndDocumentIdOrderByConfidenceDescCreatedAtDesc(userId, documentId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * 接受候选知识点并转为正式知识点。
     */
    @Transactional
    public KnowledgeConceptResponse acceptConcept(
        AuthenticatedUser authenticatedUser,
        Long conceptId,
        AcceptKnowledgeConceptRequest request
    ) {
        Long userId = getCurrentUserId(authenticatedUser);
        KnowledgeConcept concept = knowledgeConceptRepository.findByIdAndUserId(conceptId, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Knowledge concept not found"));
        if (concept.getStatus() != KnowledgeConceptStatus.CANDIDATE) {
            throw new BusinessException(HttpStatus.CONFLICT, "Knowledge concept is not in candidate status");
        }

        Long mergeKnowledgeId = request == null ? null : request.getMergeKnowledgeId();
        Knowledge savedKnowledge;
        if (mergeKnowledgeId == null) {
            savedKnowledge = createKnowledgeFromConcept(concept, request);
        } else {
            savedKnowledge = mergeConceptToExistingKnowledge(userId, concept, mergeKnowledgeId, request);
        }

        concept.setStatus(KnowledgeConceptStatus.ACCEPTED);
        concept.setAcceptedKnowledge(savedKnowledge);
        KnowledgeConcept savedConcept = knowledgeConceptRepository.save(concept);

        log.info(
            "Knowledge concept accepted: conceptId={}, documentId={}, acceptedKnowledgeId={}",
            savedConcept.getId(),
            savedConcept.getDocument() == null ? null : savedConcept.getDocument().getId(),
            savedKnowledge.getId()
        );
        return toResponse(savedConcept);
    }

    private Knowledge createKnowledgeFromConcept(KnowledgeConcept concept, AcceptKnowledgeConceptRequest request) {
        String title = normalize(request == null ? null : request.getTitle());
        if (title.isEmpty()) {
            title = normalize(concept.getName());
        }
        if (title.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "knowledge title must not be empty");
        }

        String content = normalize(request == null ? null : request.getContent());
        if (content.isEmpty()) {
            content = normalize(concept.getSummary());
        }
        if (content.isEmpty()) {
            content = title;
        }

        Knowledge knowledge = new Knowledge();
        knowledge.setUser(concept.getUser());
        knowledge.setTitle(truncate(title, 200));
        knowledge.setContent(content);
        knowledge.setMastery(0);
        knowledge.setSourceType(KnowledgeSourceType.FILE_IMPORT);
        knowledge.setStatus(KnowledgeStatus.ACTIVE);
        knowledge.replaceTags(normalizeTags(request == null ? null : request.getTags()));
        return knowledgeRepository.save(knowledge);
    }

    private Knowledge mergeConceptToExistingKnowledge(
        Long userId,
        KnowledgeConcept concept,
        Long mergeKnowledgeId,
        AcceptKnowledgeConceptRequest request
    ) {
        Knowledge targetKnowledge = knowledgeRepository.findByIdAndUserId(mergeKnowledgeId, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Knowledge not found with id: " + mergeKnowledgeId));
        if (targetKnowledge.getStatus() != KnowledgeStatus.ACTIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "Knowledge is not active");
        }

        String customTitle = normalize(request == null ? null : request.getTitle());
        if (!customTitle.isEmpty()) {
            targetKnowledge.setTitle(truncate(customTitle, 200));
        }

        String contentToMerge = normalize(request == null ? null : request.getContent());
        if (contentToMerge.isEmpty()) {
            contentToMerge = normalize(concept.getSummary());
        }
        if (!contentToMerge.isEmpty()) {
            targetKnowledge.setContent(mergeContent(normalize(targetKnowledge.getContent()), contentToMerge));
        }

        List<String> appendTags = normalizeTags(request == null ? null : request.getTags());
        if (!appendTags.isEmpty()) {
            LinkedHashSet<String> mergedTags = new LinkedHashSet<>();
            for (KnowledgeTag existingTag : targetKnowledge.getTags()) {
                String tag = normalize(existingTag.getTag()).toLowerCase(Locale.ROOT);
                if (!tag.isEmpty()) {
                    mergedTags.add(tag);
                }
            }
            mergedTags.addAll(appendTags);
            targetKnowledge.replaceTags(mergedTags);
        }

        return knowledgeRepository.save(targetKnowledge);
    }

    /**
     * 拒绝候选知识点。
     */
    @Transactional
    public KnowledgeConceptResponse rejectConcept(AuthenticatedUser authenticatedUser, Long conceptId) {
        Long userId = getCurrentUserId(authenticatedUser);
        KnowledgeConcept concept = knowledgeConceptRepository.findByIdAndUserId(conceptId, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Knowledge concept not found"));
        if (concept.getStatus() == KnowledgeConceptStatus.ACCEPTED) {
            throw new BusinessException(HttpStatus.CONFLICT, "Accepted knowledge concept cannot be rejected");
        }
        if (concept.getStatus() == KnowledgeConceptStatus.REJECTED) {
            return toResponse(concept);
        }
        concept.setStatus(KnowledgeConceptStatus.REJECTED);
        KnowledgeConcept savedConcept = knowledgeConceptRepository.save(concept);
        return toResponse(savedConcept);
    }

    /**
     * 依据文档与切片生成最小候选知识点，避免重复生成。
     */
    @Transactional
    public void createConceptCandidates(KnowledgeDocument document, List<KnowledgeChunk> chunks) {
        if (document == null || document.getId() == null || document.getUser() == null || document.getUser().getId() == null) {
            return;
        }
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        Long userId = document.getUser().getId();
        UUID documentId = document.getId();
        boolean existsCandidate = knowledgeConceptRepository.existsByUserIdAndDocumentIdAndStatus(
            userId,
            documentId,
            KnowledgeConceptStatus.CANDIDATE
        );
        if (existsCandidate) {
            return;
        }

        KnowledgeConcept concept = new KnowledgeConcept();
        concept.setUser(document.getUser());
        concept.setDocument(document);
        concept.setName(truncate(normalize(document.getTitle()), 200));
        concept.setSummary(buildSummary(chunks));
        concept.setAliases(writeJson(buildAliases(document)));
        concept.setSupportingChunkIds(writeJson(buildSupportingChunkIds(chunks)));
        concept.setConfidence(resolveConfidence(chunks.size()));
        concept.setStatus(KnowledgeConceptStatus.CANDIDATE);
        knowledgeConceptRepository.save(concept);
    }

    private void ensureDocumentOwned(Long userId, UUID documentId) {
        knowledgeDocumentRepository.findByIdAndUserId(documentId, userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Knowledge document not found"));
    }

    private KnowledgeConceptResponse toResponse(KnowledgeConcept concept) {
        KnowledgeConceptResponse response = new KnowledgeConceptResponse();
        response.setConceptId(concept.getId());
        response.setDocumentId(concept.getDocument() == null ? null : concept.getDocument().getId());
        response.setName(concept.getName());
        response.setSummary(concept.getSummary());
        response.setAliases(readStringList(concept.getAliases()));
        response.setSupportingChunkIds(readLongList(concept.getSupportingChunkIds()));
        response.setConfidence(concept.getConfidence() == null ? null : concept.getConfidence().doubleValue());
        response.setStatus(concept.getStatus() == null ? null : concept.getStatus().name());
        response.setAcceptedKnowledgeId(concept.getAcceptedKnowledge() == null ? null : concept.getAcceptedKnowledge().getId());
        response.setCreatedAt(concept.getCreatedAt());
        response.setUpdatedAt(concept.getUpdatedAt());
        return response;
    }

    private String buildSummary(List<KnowledgeChunk> chunks) {
        StringBuilder builder = new StringBuilder();
        for (KnowledgeChunk chunk : chunks) {
            String text = normalize(chunk.getText());
            if (text.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append("\n");
            }
            builder.append(text);
            if (builder.length() >= 400) {
                break;
            }
        }
        return truncate(builder.toString(), 500);
    }

    private List<String> buildAliases(KnowledgeDocument document) {
        LinkedHashSet<String> aliases = new LinkedHashSet<>();
        String fileName = normalize(document.getOriginalFileName());
        if (!fileName.isEmpty()) {
            aliases.add(truncate(fileName, 50));
        }
        String title = normalize(document.getTitle());
        if (!title.isEmpty()) {
            aliases.add(truncate(title, 50));
        }
        return List.copyOf(aliases);
    }

    private List<Long> buildSupportingChunkIds(List<KnowledgeChunk> chunks) {
        List<Long> chunkIds = new ArrayList<>();
        for (KnowledgeChunk chunk : chunks) {
            if (chunk == null || chunk.getId() == null) {
                continue;
            }
            chunkIds.add(chunk.getId());
            if (chunkIds.size() >= 5) {
                break;
            }
        }
        return chunkIds;
    }

    private BigDecimal resolveConfidence(int chunkCount) {
        double value = Math.min(0.95d, 0.45d + (chunkCount * 0.03d));
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String value = normalize(tag).toLowerCase(Locale.ROOT);
            if (value.isEmpty()) {
                continue;
            }
            if (value.length() > 50) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "tag length must be <= 50");
            }
            normalized.add(value);
        }
        return List.copyOf(normalized);
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read knowledge concept aliases");
        }
    }

    private List<Long> readLongList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, LONG_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read knowledge concept chunks");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist knowledge concept metadata");
        }
    }

    private Long getCurrentUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authenticatedUser.getId();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String truncate(String value, int limit) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.length() <= limit ? value : value.substring(0, limit);
    }

    private String mergeContent(String existing, String appended) {
        String normalizedExisting = normalize(existing);
        String normalizedAppended = normalize(appended);
        if (normalizedExisting.isEmpty()) {
            return normalizedAppended;
        }
        if (normalizedAppended.isEmpty()) {
            return normalizedExisting;
        }
        if (normalizedExisting.contains(normalizedAppended)) {
            return normalizedExisting;
        }
        return normalizedExisting + "\n\n" + normalizedAppended;
    }
}
