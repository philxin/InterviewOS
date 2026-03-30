package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.knowledge.AcceptKnowledgeConceptRequest;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeConceptResponse;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.KnowledgeConceptService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 候选知识点接口：查询、接受、拒绝。
 */
@RestController
@RequestMapping("/knowledge")
public class KnowledgeConceptController {
    private final KnowledgeConceptService knowledgeConceptService;

    public KnowledgeConceptController(KnowledgeConceptService knowledgeConceptService) {
        this.knowledgeConceptService = knowledgeConceptService;
    }

    /**
     * 查询指定文档的候选知识点列表。
     */
    @GetMapping("/documents/{documentId}/concepts")
    public ResponseEntity<Result<List<KnowledgeConceptResponse>>> listDocumentConcepts(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable UUID documentId
    ) {
        return ResponseEntity.ok(Result.success(knowledgeConceptService.listDocumentConcepts(authenticatedUser, documentId)));
    }

    /**
     * 接受候选知识点并转为正式知识点。
     */
    @PostMapping("/concepts/{conceptId}/accept")
    public ResponseEntity<Result<KnowledgeConceptResponse>> acceptConcept(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long conceptId,
        @Valid @RequestBody(required = false) AcceptKnowledgeConceptRequest request
    ) {
        return ResponseEntity.ok(Result.success(knowledgeConceptService.acceptConcept(authenticatedUser, conceptId, request)));
    }

    /**
     * 拒绝候选知识点。
     */
    @PostMapping("/concepts/{conceptId}/reject")
    public ResponseEntity<Result<KnowledgeConceptResponse>> rejectConcept(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable Long conceptId
    ) {
        return ResponseEntity.ok(Result.success(knowledgeConceptService.rejectConcept(authenticatedUser, conceptId)));
    }
}
