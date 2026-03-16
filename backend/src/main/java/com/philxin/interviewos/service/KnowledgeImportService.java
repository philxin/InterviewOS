package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.knowledge.BatchImportKnowledgeItemRequest;
import com.philxin.interviewos.controller.dto.knowledge.BatchImportKnowledgeResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.repository.KnowledgeRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 知识点批量导入服务，负责逐项校验与失败项汇总。
 */
@Service
public class KnowledgeImportService {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeImportService.class);

    private final AppUserRepository appUserRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final Validator validator;

    public KnowledgeImportService(
        AppUserRepository appUserRepository,
        KnowledgeRepository knowledgeRepository,
        Validator validator
    ) {
        this.appUserRepository = appUserRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.validator = validator;
    }

    /**
     * 批量导入当前用户的知识点，允许部分成功并返回逐项失败原因。
     */
    public BatchImportKnowledgeResponse batchImportKnowledge(
        AuthenticatedUser authenticatedUser,
        List<BatchImportKnowledgeItemRequest> items
    ) {
        AppUser user = getCurrentUserEntity(authenticatedUser);
        BatchImportKnowledgeResponse response = new BatchImportKnowledgeResponse();

        for (int index = 0; index < items.size(); index++) {
            BatchImportKnowledgeItemRequest item = items.get(index);
            String validationMessage = validateItem(item);
            if (validationMessage != null) {
                appendFailedItem(response, index, item == null ? null : normalize(item.getTitle()), validationMessage);
                continue;
            }

            try {
                Knowledge knowledge = new Knowledge();
                knowledge.setUser(user);
                knowledge.setTitle(normalize(item.getTitle()));
                knowledge.setContent(normalize(item.getContent()));
                knowledge.setMastery(0);
                knowledge.setSourceType(KnowledgeSourceType.BATCH_IMPORT);
                knowledge.setStatus(KnowledgeStatus.ACTIVE);
                knowledge.replaceTags(normalizeTags(item.getTags()));
                knowledgeRepository.save(knowledge);
                response.setCreatedCount(response.getCreatedCount() + 1);
            } catch (RuntimeException exception) {
                log.warn("Batch import item failed: index={}, title={}", index, normalize(item.getTitle()), exception);
                appendFailedItem(response, index, normalize(item.getTitle()), "Failed to import item");
            }
        }

        response.setFailedCount(response.getFailedItems().size());
        return response;
    }

    private void appendFailedItem(
        BatchImportKnowledgeResponse response,
        int index,
        String title,
        String reason
    ) {
        BatchImportKnowledgeResponse.FailedItem failedItem = new BatchImportKnowledgeResponse.FailedItem();
        failedItem.setIndex(index);
        failedItem.setTitle(title);
        failedItem.setReason(reason);
        response.getFailedItems().add(failedItem);
    }

    private String validateItem(BatchImportKnowledgeItemRequest item) {
        if (item == null) {
            return "item must not be null";
        }

        Set<ConstraintViolation<BatchImportKnowledgeItemRequest>> violations = validator.validate(item);
        return violations.stream()
            .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
            .findFirst()
            .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
            .orElse(null);
    }

    private AppUser getCurrentUserEntity(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return appUserRepository.findById(authenticatedUser.getId())
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalizedTag = normalize(tag);
            if (normalizedTag == null || normalizedTag.isBlank()) {
                continue;
            }
            normalizedTags.add(normalizedTag.toLowerCase(Locale.ROOT));
        }
        return List.copyOf(normalizedTags);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
