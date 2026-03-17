package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.training.StartTrainingSessionRequest;
import com.philxin.interviewos.controller.dto.training.SubmitSessionAnswerRequest;
import com.philxin.interviewos.controller.dto.training.TrainingFeedbackResponse;
import com.philxin.interviewos.controller.dto.training.TrainingHintResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionDetailResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionListResponse;
import com.philxin.interviewos.controller.dto.training.TrainingSessionStartResponse;
import com.philxin.interviewos.security.AuthenticatedUser;
import com.philxin.interviewos.service.TrainingSessionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * V2 训练会话接口。
 */
@Validated
@RestController
@RequestMapping("/training/sessions")
public class TrainingSessionController {
    private final TrainingSessionService trainingSessionService;

    public TrainingSessionController(TrainingSessionService trainingSessionService) {
        this.trainingSessionService = trainingSessionService;
    }

    @PostMapping
    public ResponseEntity<Result<TrainingSessionStartResponse>> startSession(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @Valid @RequestBody StartTrainingSessionRequest request
    ) {
        return ResponseEntity.ok(Result.success(trainingSessionService.startSession(authenticatedUser, request)));
    }

    @PostMapping("/{sessionId}/answers")
    public ResponseEntity<Result<TrainingFeedbackResponse>> submitAnswer(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable UUID sessionId,
        @Valid @RequestBody SubmitSessionAnswerRequest request
    ) {
        return ResponseEntity.ok(Result.success(trainingSessionService.submitAnswer(authenticatedUser, sessionId, request)));
    }

    @PostMapping("/{sessionId}/questions/{questionId}/hint")
    public ResponseEntity<Result<TrainingHintResponse>> getHint(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable UUID sessionId,
        @PathVariable UUID questionId
    ) {
        return ResponseEntity.ok(Result.success(trainingSessionService.getHint(authenticatedUser, sessionId, questionId)));
    }

    @GetMapping
    public ResponseEntity<Result<TrainingSessionListResponse>> getSessionHistory(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @RequestParam(required = false) Long knowledgeId,
        @RequestParam(required = false) Integer page,
        @RequestParam(required = false) Integer size
    ) {
        return ResponseEntity.ok(Result.success(
            trainingSessionService.getSessionHistory(authenticatedUser, knowledgeId, page, size)
        ));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<Result<TrainingSessionDetailResponse>> getSessionDetail(
        @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
        @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(Result.success(trainingSessionService.getSessionDetail(authenticatedUser, sessionId)));
    }
}
