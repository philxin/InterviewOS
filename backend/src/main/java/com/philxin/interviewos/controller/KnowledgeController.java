package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.knowledge.BatchImportKnowledgeRequest;
import com.philxin.interviewos.controller.dto.knowledge.BatchImportKnowledgeResponse;
import com.philxin.interviewos.controller.dto.knowledge.CreateKnowledgeRequest;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeFileImportResponse;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeFileImportStartResponse;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeResponse;
import com.philxin.interviewos.controller.dto.knowledge.UpdateKnowledgeRequest;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.KnowledgeFileImportService;
import com.philxin.interviewos.service.KnowledgeImportService;
import com.philxin.interviewos.service.KnowledgeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {
    private final KnowledgeFileImportService knowledgeFileImportService;
    private final KnowledgeImportService knowledgeImportService;
    private final KnowledgeService knowledgeService;

    public KnowledgeController(
        KnowledgeService knowledgeService,
        KnowledgeImportService knowledgeImportService,
        KnowledgeFileImportService knowledgeFileImportService
    ) {
        this.knowledgeService = knowledgeService;
        this.knowledgeImportService = knowledgeImportService;
        this.knowledgeFileImportService = knowledgeFileImportService;
    }

    /**
     * 获取知识点列表。
     */
    @GetMapping
    public ResponseEntity<Result<List<KnowledgeResponse>>> getKnowledgeList(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        List<KnowledgeResponse> response = knowledgeService.getKnowledgeList(authenticatedUser)
            .stream()
            .map(KnowledgeResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(Result.success(response));
    }

    /**
     * 按 ID 获取知识点详情。
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<KnowledgeResponse>> getKnowledgeById(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable @Min(1) Long id
    ) {
        Knowledge knowledge = knowledgeService.getKnowledgeById(authenticatedUser, id);
        return ResponseEntity.ok(Result.success(KnowledgeResponse.fromEntity(knowledge)));
    }

    /**
     * 获取当前用户可见标签列表。
     */
    @GetMapping("/tags")
    public ResponseEntity<Result<List<String>>> getKnowledgeTags(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ResponseEntity.ok(Result.success(knowledgeService.getKnowledgeTags(authenticatedUser)));
    }

    /**
     * 创建知识点。
     */
    @PostMapping
    public ResponseEntity<Result<KnowledgeResponse>> createKnowledge(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody CreateKnowledgeRequest request
    ) {
        Knowledge knowledge = knowledgeService.createKnowledge(
            authenticatedUser,
            request.getTitle(),
            request.getContent(),
            request.getTags()
        );
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(Result.success(KnowledgeResponse.fromEntity(knowledge)));
    }

    /**
     * 批量导入知识点，允许部分成功并返回失败项。
     */
    @PostMapping("/batch-import")
    public ResponseEntity<Result<BatchImportKnowledgeResponse>> batchImportKnowledge(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody BatchImportKnowledgeRequest request
    ) {
        BatchImportKnowledgeResponse response = knowledgeImportService.batchImportKnowledge(
            authenticatedUser,
            request.getItems()
        );
        if (response.allFailed()) {
            return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Result.of(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Batch import validation failed", response));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Result.success(response));
    }

    /**
     * 创建文件导入任务，文件解析在后台异步执行。
     */
    @PostMapping("/file-imports")
    public ResponseEntity<Result<KnowledgeFileImportStartResponse>> createKnowledgeFileImport(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "defaultTags", required = false) String defaultTags
    ) {
        KnowledgeFileImportStartResponse response = knowledgeFileImportService.createFileImport(
            authenticatedUser,
            file,
            defaultTags
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Result.success(response));
    }

    /**
     * 查询文件导入任务状态。
     */
    @GetMapping("/file-imports/{importId}")
    public ResponseEntity<Result<KnowledgeFileImportResponse>> getKnowledgeFileImport(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable UUID importId
    ) {
        return ResponseEntity.ok(Result.success(knowledgeFileImportService.getFileImport(authenticatedUser, importId)));
    }

    /**
     * 更新知识点。
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<KnowledgeResponse>> updateKnowledge(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable @Min(1) Long id,
        @Valid @RequestBody UpdateKnowledgeRequest request
    ) {
        Knowledge knowledge = knowledgeService.updateKnowledge(
            authenticatedUser,
            id,
            request.getTitle(),
            request.getContent(),
            request.getTags()
        );
        return ResponseEntity.ok(Result.success(KnowledgeResponse.fromEntity(knowledge)));
    }

    /**
     * 删除知识点。
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteKnowledge(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable @Min(1) Long id
    ) {
        knowledgeService.deleteKnowledge(authenticatedUser, id);
        return ResponseEntity.ok(Result.success());
    }
}
