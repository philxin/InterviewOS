package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeDocumentResponse;
import com.philxin.interviewos.entity.KnowledgeDocument;
import com.philxin.interviewos.repository.KnowledgeDocumentRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文档级知识库查询服务。
 */
@Service
public class KnowledgeDocumentService {
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public KnowledgeDocumentService(KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listDocuments(AuthenticatedUser authenticatedUser) {
        return knowledgeDocumentRepository.findByUserIdOrderByUpdatedAtDesc(getCurrentUserId(authenticatedUser))
            .stream()
            .map(KnowledgeDocumentResponse::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse getDocument(AuthenticatedUser authenticatedUser, UUID documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findByIdAndUserId(documentId, getCurrentUserId(authenticatedUser))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Knowledge document not found"));
        return KnowledgeDocumentResponse.fromEntity(document);
    }

    private Long getCurrentUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authenticatedUser.getId();
    }
}
