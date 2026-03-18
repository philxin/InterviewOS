package com.philxin.interviewos.service;

import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeFileImport;
import com.philxin.interviewos.entity.KnowledgeFileImportStatus;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.repository.KnowledgeFileImportRepository;
import com.philxin.interviewos.repository.KnowledgeRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文件导入异步处理器，负责解析文件并创建知识点。
 */
@Service
public class KnowledgeFileImportProcessor {
    private static final Logger log = LoggerFactory.getLogger(KnowledgeFileImportProcessor.class);

    private final KnowledgeFileImportRepository knowledgeFileImportRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeFileImportParser parser;

    public KnowledgeFileImportProcessor(
        KnowledgeFileImportRepository knowledgeFileImportRepository,
        KnowledgeRepository knowledgeRepository,
        KnowledgeFileImportParser parser
    ) {
        this.knowledgeFileImportRepository = knowledgeFileImportRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.parser = parser;
    }

    /**
     * 异步执行文件导入，避免上传请求长时间阻塞。
     */
    @Async("knowledgeFileImportExecutor")
    @Transactional
    public void processImport(UUID importId, byte[] content, List<String> defaultTags) {
        KnowledgeFileImport fileImport = knowledgeFileImportRepository.findById(importId)
            .orElseThrow(() -> new IllegalStateException("Knowledge file import not found: " + importId));
        fileImport.setStatus(KnowledgeFileImportStatus.PROCESSING);
        fileImport.setFailureReason(null);
        knowledgeFileImportRepository.save(fileImport);

        try {
            String parsedContent = parser.extractText(fileImport.getFileName(), content);
            if (parsedContent.isBlank()) {
                throw new IllegalArgumentException("Parsed file content is empty");
            }

            Knowledge knowledge = buildKnowledge(fileImport.getUser(), fileImport.getFileName(), parsedContent, defaultTags);
            knowledgeRepository.save(knowledge);

            fileImport.setCreatedCount(1);
            fileImport.setStatus(KnowledgeFileImportStatus.SUCCESS);
            fileImport.setCompletedAt(LocalDateTime.now());
            knowledgeFileImportRepository.save(fileImport);
            log.info("Knowledge file import succeeded: importId={}, fileName={}", importId, fileImport.getFileName());
        } catch (RuntimeException exception) {
            fileImport.setCreatedCount(0);
            fileImport.setStatus(KnowledgeFileImportStatus.FAILED);
            fileImport.setFailureReason(truncate(exception.getMessage()));
            fileImport.setCompletedAt(LocalDateTime.now());
            knowledgeFileImportRepository.save(fileImport);
            log.warn("Knowledge file import failed: importId={}, fileName={}", importId, fileImport.getFileName(), exception);
        }
    }

    private Knowledge buildKnowledge(AppUser user, String fileName, String content, List<String> defaultTags) {
        Knowledge knowledge = new Knowledge();
        knowledge.setUser(user);
        knowledge.setTitle(parser.resolveTitle(fileName));
        knowledge.setContent(content.trim());
        knowledge.setMastery(0);
        knowledge.setSourceType(KnowledgeSourceType.FILE_IMPORT);
        knowledge.setStatus(KnowledgeStatus.ACTIVE);
        knowledge.replaceTags(defaultTags);
        return knowledge;
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "File import failed";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
