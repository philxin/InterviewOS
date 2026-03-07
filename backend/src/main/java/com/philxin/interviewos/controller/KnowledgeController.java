package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.knowledge.CreateKnowledgeRequest;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeResponse;
import com.philxin.interviewos.controller.dto.knowledge.UpdateKnowledgeRequest;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.service.KnowledgeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {
    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    /**
     * 获取知识点列表。
     */
    @GetMapping
    public ResponseEntity<Result<List<KnowledgeResponse>>> getKnowledgeList() {
        List<KnowledgeResponse> response = knowledgeService.getKnowledgeList()
            .stream()
            .map(KnowledgeResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(Result.success(response));
    }

    /**
     * 按 ID 获取知识点详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<KnowledgeResponse>> getKnowledgeById(@PathVariable @Min(1) Long id) {
        Knowledge knowledge = knowledgeService.getKnowledgeById(id);
        return ResponseEntity.ok(Result.success(KnowledgeResponse.fromEntity(knowledge)));
    }

    /**
     * 创建知识点。
     */
    @PostMapping
    public ResponseEntity<Result<KnowledgeResponse>> createKnowledge(
        @Valid @RequestBody CreateKnowledgeRequest request
    ) {
        Knowledge knowledge = knowledgeService.createKnowledge(request.getTitle(), request.getContent());
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Result.success(KnowledgeResponse.fromEntity(knowledge)));
    }

    /**
     * 更新知识点。
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<KnowledgeResponse>> updateKnowledge(
        @PathVariable @Min(1) Long id,
        @Valid @RequestBody UpdateKnowledgeRequest request
    ) {
        Knowledge knowledge = knowledgeService.updateKnowledge(id, request.getTitle(), request.getContent());
        return ResponseEntity.ok(Result.success(KnowledgeResponse.fromEntity(knowledge)));
    }

    /**
     * 删除知识点。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteKnowledge(@PathVariable @Min(1) Long id) {
        knowledgeService.deleteKnowledge(id);
        return ResponseEntity.ok(Result.success());
    }
}
