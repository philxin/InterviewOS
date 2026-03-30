package com.philxin.interviewos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.KnowledgeChunk;
import com.philxin.interviewos.entity.KnowledgeChunkStatus;
import com.philxin.interviewos.entity.KnowledgeDocument;
import com.philxin.interviewos.entity.KnowledgeDocumentStatus;
import com.philxin.interviewos.entity.Knowledge;
import com.philxin.interviewos.entity.KnowledgeFileImport;
import com.philxin.interviewos.entity.KnowledgeFileImportStatus;
import com.philxin.interviewos.entity.KnowledgeSourceType;
import com.philxin.interviewos.entity.KnowledgeStatus;
import com.philxin.interviewos.repository.KnowledgeChunkRepository;
import com.philxin.interviewos.repository.KnowledgeDocumentRepository;
import com.philxin.interviewos.repository.KnowledgeFileImportRepository;
import com.philxin.interviewos.repository.KnowledgeRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeFileImportParser parser;
    private final KnowledgeChunkingService knowledgeChunkingService;
    private final EmbeddingService embeddingService;
    private final RetrievalService retrievalService;
    private final KnowledgeConceptService knowledgeConceptService;
    private final ObjectMapper objectMapper;

    public KnowledgeFileImportProcessor(
        KnowledgeFileImportRepository knowledgeFileImportRepository,
        KnowledgeRepository knowledgeRepository,
        KnowledgeDocumentRepository knowledgeDocumentRepository,
        KnowledgeChunkRepository knowledgeChunkRepository,
        KnowledgeFileImportParser parser,
        KnowledgeChunkingService knowledgeChunkingService,
        EmbeddingService embeddingService,
        RetrievalService retrievalService,
        KnowledgeConceptService knowledgeConceptService,
        ObjectMapper objectMapper
    ) {
        this.knowledgeFileImportRepository = knowledgeFileImportRepository;
        this.knowledgeRepository = knowledgeRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.parser = parser;
        this.knowledgeChunkingService = knowledgeChunkingService;
        this.embeddingService = embeddingService;
        this.retrievalService = retrievalService;
        this.knowledgeConceptService = knowledgeConceptService;
        this.objectMapper = objectMapper;
    }

    /**
     * 异步执行文件导入，避免上传请求长时间阻塞。
     */
    @Async("knowledgeFileImportExecutor")
    @Transactional
    public void processImport(UUID importId, byte[] content, List<String> defaultTags) {
        KnowledgeFileImport fileImport = knowledgeFileImportRepository.findById(importId)
            .orElseThrow(() -> new IllegalStateException("Knowledge file import not found: " + importId));
        if (fileImport.getStatus() == KnowledgeFileImportStatus.SUCCESS || fileImport.getStatus() == KnowledgeFileImportStatus.PARTIAL) {
            log.info("Skip already completed import: importId={}, status={}", importId, fileImport.getStatus());
            return;
        }
        fileImport.setStatus(KnowledgeFileImportStatus.PROCESSING);
        fileImport.setFailureReason(null);
        fileImport.setCompletedAt(null);
        knowledgeFileImportRepository.save(fileImport);

        try {
            String parsedContent = knowledgeChunkingService.normalize(parser.extractText(fileImport.getFileName(), content));
            if (parsedContent.isBlank()) {
                throw new IllegalArgumentException("Parsed file content is empty");
            }
            String contentHash = hash(parsedContent);
            fileImport.setContentHash(contentHash);
            fileImport.setParserVersion(KnowledgeFileImportParser.PARSER_VERSION);
            fileImport.setEmbeddingModel(embeddingService.getModel());
            fileImport.setEmbeddingDim(embeddingService.getDimensions());

            Knowledge knowledge = buildKnowledge(fileImport.getUser(), fileImport.getFileName(), parsedContent, defaultTags);
            knowledgeRepository.save(knowledge);

            KnowledgeDocument savedDocument = knowledgeDocumentRepository.save(buildDocument(fileImport, parsedContent, contentHash));
            fileImport.setDocumentId(savedDocument.getId());
            knowledgeFileImportRepository.save(fileImport);

            fileImport.setStatus(KnowledgeFileImportStatus.CHUNKING);
            List<KnowledgeChunkingService.ChunkDraft> chunkDrafts = knowledgeChunkingService.split(parsedContent);
            fileImport.setTotalChunks(chunkDrafts.size());
            savedDocument.setTotalChunks(chunkDrafts.size());
            knowledgeFileImportRepository.save(fileImport);

            List<KnowledgeChunk> chunks = chunkDrafts.stream()
                .map(chunkDraft -> buildChunk(savedDocument, fileImport.getUser(), chunkDraft))
                .toList();
            knowledgeChunkRepository.saveAll(chunks);
            knowledgeConceptService.createConceptCandidates(savedDocument, chunks);

            fileImport.setStatus(KnowledgeFileImportStatus.EMBEDDING);
            knowledgeFileImportRepository.save(fileImport);

            int embeddedChunks = 0;
            int failedChunks = 0;
            for (KnowledgeChunk chunk : chunks) {
                try {
                    float[] embedding = embeddingService.embed(chunk.getText());
                    chunk.setEmbedding(retrievalService.writeEmbedding(embedding));
                    chunk.setEmbeddingModel(embeddingService.getModel());
                    chunk.setEmbeddingDim(embeddingService.getDimensions());
                    chunk.setStatus(KnowledgeChunkStatus.READY);
                    chunk.setFailureReason(null);
                    embeddedChunks++;
                } catch (RuntimeException exception) {
                    chunk.setStatus(KnowledgeChunkStatus.FAILED);
                    chunk.setFailureReason(sanitizeFailureReason(exception, 255));
                    failedChunks++;
                    log.warn(
                        "Knowledge chunk embedding failed: importId={}, documentId={}, chunkIndex={}",
                        importId,
                        savedDocument.getId(),
                        chunk.getChunkIndex(),
                        exception
                    );
                }
                knowledgeChunkRepository.save(chunk);
            }

            fileImport.setEmbeddedChunks(embeddedChunks);
            fileImport.setFailedChunks(failedChunks);
            fileImport.setCreatedCount(1);
            fileImport.setStatus(resolveImportStatus(embeddedChunks, failedChunks));
            fileImport.setCompletedAt(LocalDateTime.now());
            savedDocument.setActiveChunks(embeddedChunks);
            savedDocument.setIndexedAt(LocalDateTime.now());
            savedDocument.setStatus(embeddedChunks > 0 ? KnowledgeDocumentStatus.ACTIVE : KnowledgeDocumentStatus.FAILED);
            knowledgeDocumentRepository.save(savedDocument);
            knowledgeFileImportRepository.save(fileImport);
            log.info(
                "Knowledge file import indexed: importId={}, fileName={}, documentId={}, totalChunks={}, embeddedChunks={}, failedChunks={}, status={}",
                importId,
                fileImport.getFileName(),
                savedDocument.getId(),
                chunkDrafts.size(),
                embeddedChunks,
                failedChunks,
                fileImport.getStatus()
            );
        } catch (RuntimeException exception) {
            fileImport.setCreatedCount(0);
            fileImport.setStatus(KnowledgeFileImportStatus.FAILED);
            fileImport.setFailureReason(sanitizeFailureReason(exception, 1000));
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

    private KnowledgeDocument buildDocument(KnowledgeFileImport fileImport, String content, String contentHash) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setUser(fileImport.getUser());
        document.setFileImport(fileImport);
        document.setTitle(parser.resolveTitle(fileImport.getFileName()));
        document.setOriginalFileName(fileImport.getFileName());
        document.setContentType(fileImport.getContentType());
        document.setContentHash(contentHash);
        document.setParserVersion(KnowledgeFileImportParser.PARSER_VERSION);
        document.setChunkStrategy(knowledgeChunkingService.getChunkStrategy());
        document.setEmbeddingModel(embeddingService.getModel());
        document.setEmbeddingDim(embeddingService.getDimensions());
        document.setStatus(KnowledgeDocumentStatus.PROCESSING);
        Map<String, Object> sourceMeta = new LinkedHashMap<>();
        sourceMeta.put("contentLength", content.length());
        sourceMeta.put("fileSize", fileImport.getFileSize());
        document.setSourceMeta(writeJson(sourceMeta));
        return document;
    }

    private KnowledgeChunk buildChunk(
        KnowledgeDocument document,
        AppUser user,
        KnowledgeChunkingService.ChunkDraft chunkDraft
    ) {
        KnowledgeChunk chunk = new KnowledgeChunk();
        chunk.setDocument(document);
        chunk.setUser(user);
        chunk.setChunkIndex(chunkDraft.chunkIndex());
        chunk.setText(chunkDraft.text());
        chunk.setTextHash(hash(chunkDraft.text()));
        chunk.setCharCount(chunkDraft.charCount());
        chunk.setTokenCount(chunkDraft.tokenCount());
        chunk.setStartOffset(chunkDraft.startOffset());
        chunk.setEndOffset(chunkDraft.endOffset());
        chunk.setStatus(KnowledgeChunkStatus.PENDING);
        return chunk;
    }

    private KnowledgeFileImportStatus resolveImportStatus(int embeddedChunks, int failedChunks) {
        if (embeddedChunks > 0 && failedChunks == 0) {
            return KnowledgeFileImportStatus.SUCCESS;
        }
        if (embeddedChunks > 0) {
            return KnowledgeFileImportStatus.PARTIAL;
        }
        return KnowledgeFileImportStatus.FAILED;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize document source metadata", exception);
        }
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 not available", exception);
        }
    }

    private String truncate(String message, int limit) {
        if (message == null || message.isBlank()) {
            return "File import failed";
        }
        return message.length() <= limit ? message : message.substring(0, limit);
    }

    private String sanitizeFailureReason(Throwable exception, int limit) {
        if (exception == null || exception.getMessage() == null || exception.getMessage().isBlank()) {
            return "File import failed";
        }
        String raw = exception.getMessage().replace("\r\n", "\n").replace("\r", "\n");
        if (looksLikeStackTrace(raw)) {
            return "File import failed due to parser or indexing error";
        }
        String firstLine = raw.split("\n", 2)[0].trim().replaceAll("\\s+", " ");
        if (firstLine.isBlank()) {
            return "File import failed";
        }
        return truncate(firstLine, limit);
    }

    private boolean looksLikeStackTrace(String message) {
        String normalized = message == null ? "" : message;
        return normalized.contains("\n\tat ")
            || normalized.contains("\nat ")
            || normalized.contains("Caused by:")
            || normalized.contains("java.lang.")
            || normalized.contains("org.springframework.");
    }
}
