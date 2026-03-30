package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.rag.RagSearchItemResponse;
import com.philxin.interviewos.controller.dto.rag.RagSearchResponse;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.RetrievalService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * V3 最小可用 RAG 检索接口。
 */
@Validated
@RestController
@RequestMapping("/rag")
public class RagController {
    private final RetrievalService retrievalService;

    public RagController(RetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @GetMapping("/search")
    public ResponseEntity<Result<RagSearchResponse>> search(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam String query,
        @RequestParam(required = false) UUID documentId,
        @RequestParam(required = false) Integer topK
    ) {
        RetrievalService.RetrievalResult result = retrievalService.search(authenticatedUser, query, documentId, topK);
        RagSearchResponse response = new RagSearchResponse();
        response.setQuery(result.query());
        response.setDocumentId(result.documentId());
        response.setTopK(result.topK());
        response.setHitCount(result.matches().size());
        response.setDegraded(result.degraded());
        response.setItems(result.matches().stream().map(match -> {
            RagSearchItemResponse item = new RagSearchItemResponse();
            item.setChunkId(match.chunk().getId());
            item.setDocumentId(match.document().getId());
            item.setDocumentTitle(match.document().getTitle());
            item.setScore(match.score());
            item.setSnippet(match.chunk().getText());
            item.setPageFrom(match.chunk().getPageFrom());
            item.setPageTo(match.chunk().getPageTo());
            item.setStartOffset(match.chunk().getStartOffset());
            item.setEndOffset(match.chunk().getEndOffset());
            return item;
        }).toList());
        return ResponseEntity.ok(Result.success(response));
    }
}
