package com.philxin.interviewos.controller;

import com.philxin.interviewos.common.Result;
import com.philxin.interviewos.controller.dto.training.StartTrainingRequest;
import com.philxin.interviewos.controller.dto.training.StartTrainingResponse;
import com.philxin.interviewos.controller.dto.training.SubmitTrainingRequest;
import com.philxin.interviewos.controller.dto.training.SubmitTrainingResponse;
import com.philxin.interviewos.controller.dto.training.TrainingRecordResponse;
import com.philxin.interviewos.entity.TrainingRecord;
import com.philxin.interviewos.llm.EvaluationResult;
import com.philxin.interviewos.service.TrainingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/training")
public class TrainingController {
    private final TrainingService trainingService;

    public TrainingController(TrainingService trainingService) {
        this.trainingService = trainingService;
    }

    /**
     * 开始训练，基于知识点生成问题。
     */
    @PostMapping("/start")
    public ResponseEntity<Result<StartTrainingResponse>> startTraining(
        @Valid @RequestBody StartTrainingRequest request
    ) {
        String question = trainingService.startTraining(request.getKnowledgeId());
        return ResponseEntity.ok(Result.success(new StartTrainingResponse(question)));
    }

    /**
     * 提交回答，返回结构化评分结果。
     */
    @PostMapping("/submit")
    public ResponseEntity<Result<SubmitTrainingResponse>> submitAnswer(
        @Valid @RequestBody SubmitTrainingRequest request
    ) {
        EvaluationResult evaluation = trainingService.submitAnswer(
            request.getKnowledgeId(),
            request.getQuestion(),
            request.getAnswer()
        );
        return ResponseEntity.ok(Result.success(SubmitTrainingResponse.fromEvaluationResult(evaluation)));
    }

    /**
     * 查询指定知识点训练历史。
     */
    @GetMapping("/history/{knowledgeId}")
    public ResponseEntity<Result<List<TrainingRecordResponse>>> getHistoryByKnowledgeId(
        @PathVariable @Min(1) Long knowledgeId
    ) {
        List<TrainingRecordResponse> data = trainingService.getHistoryByKnowledgeId(knowledgeId)
            .stream()
            .map(TrainingRecordResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(Result.success(data));
    }

    /**
     * 查询全量训练历史。
     */
    @GetMapping("/history")
    public ResponseEntity<Result<List<TrainingRecordResponse>>> getAllHistory() {
        List<TrainingRecordResponse> data = trainingService.getAllHistory()
            .stream()
            .map(TrainingRecordResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(Result.success(data));
    }
}
