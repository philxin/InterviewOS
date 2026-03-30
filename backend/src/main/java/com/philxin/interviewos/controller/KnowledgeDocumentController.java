package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeDocumentResponse;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.KnowledgeDocumentService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * V3 文档级知识库接口。
 */
@RestController
@RequestMapping("/knowledge/documents")
public class KnowledgeDocumentController {
    private final KnowledgeDocumentService knowledgeDocumentService;

    public KnowledgeDocumentController(KnowledgeDocumentService knowledgeDocumentService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    @GetMapping
    public ResponseEntity<Result<List<KnowledgeDocumentResponse>>> listDocuments(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(Result.success(knowledgeDocumentService.listDocuments(authenticatedUser)));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<Result<KnowledgeDocumentResponse>> getDocument(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable UUID documentId
    ) {
        return ResponseEntity.ok(Result.success(knowledgeDocumentService.getDocument(authenticatedUser, documentId)));
    }
}
