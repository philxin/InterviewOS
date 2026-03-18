package com.philxin.interviewos.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeFileImportResponse;
import com.philxin.interviewos.controller.dto.knowledge.KnowledgeFileImportStartResponse;
import com.philxin.interviewos.entity.AppUser;
import com.philxin.interviewos.entity.KnowledgeFileImport;
import com.philxin.interviewos.entity.KnowledgeFileImportStatus;
import com.philxin.interviewos.repository.AppUserRepository;
import com.philxin.interviewos.repository.KnowledgeFileImportRepository;
import com.philxin.interviewos.security.AuthenticatedUser;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件导入任务服务：创建任务、校验文件并查询状态。
 */
@Service
public class KnowledgeFileImportService {
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final AppUserRepository appUserRepository;
    private final KnowledgeFileImportRepository knowledgeFileImportRepository;
    private final KnowledgeFileImportProcessor knowledgeFileImportProcessor;
    private final KnowledgeFileImportParser parser;
    private final ObjectMapper objectMapper;

    public KnowledgeFileImportService(
        AppUserRepository appUserRepository,
        KnowledgeFileImportRepository knowledgeFileImportRepository,
        KnowledgeFileImportProcessor knowledgeFileImportProcessor,
        KnowledgeFileImportParser parser,
        ObjectMapper objectMapper
    ) {
        this.appUserRepository = appUserRepository;
        this.knowledgeFileImportRepository = knowledgeFileImportRepository;
        this.knowledgeFileImportProcessor = knowledgeFileImportProcessor;
        this.parser = parser;
        this.objectMapper = objectMapper;
    }

    /**
     * 创建文件导入任务并异步启动处理。
     */
    @Transactional
    public KnowledgeFileImportStartResponse createFileImport(
        AuthenticatedUser authenticatedUser,
        MultipartFile file,
        String defaultTags
    ) {
        AppUser user = getCurrentUserEntity(authenticatedUser);
        validateFile(file);
        List<String> normalizedDefaultTags = normalizeTags(defaultTags);
        byte[] content = readFileBytes(file);

        KnowledgeFileImport fileImport = new KnowledgeFileImport();
        fileImport.setUser(user);
        fileImport.setFileName(file.getOriginalFilename().trim());
        fileImport.setContentType(resolveContentType(file));
        fileImport.setFileSize(file.getSize());
        fileImport.setStatus(KnowledgeFileImportStatus.PENDING);
        fileImport.setDefaultTags(writeJson(normalizedDefaultTags));
        fileImport = knowledgeFileImportRepository.save(fileImport);

        knowledgeFileImportProcessor.processImport(fileImport.getId(), content, normalizedDefaultTags);
        return KnowledgeFileImportStartResponse.fromEntity(fileImport);
    }

    /**
     * 查询当前用户的导入任务状态。
     */
    @Transactional(readOnly = true)
    public KnowledgeFileImportResponse getFileImport(AuthenticatedUser authenticatedUser, UUID importId) {
        KnowledgeFileImport fileImport = knowledgeFileImportRepository.findByIdAndUserId(importId, getCurrentUserId(authenticatedUser))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Knowledge file import not found"));

        KnowledgeFileImportResponse response = new KnowledgeFileImportResponse();
        response.setImportId(fileImport.getId());
        response.setFileName(fileImport.getFileName());
        response.setContentType(fileImport.getContentType());
        response.setFileSize(fileImport.getFileSize() == null ? 0L : fileImport.getFileSize());
        response.setStatus(fileImport.getStatus().name());
        response.setDefaultTags(readJson(fileImport.getDefaultTags()));
        response.setCreatedCount(fileImport.getCreatedCount() == null ? 0 : fileImport.getCreatedCount());
        response.setFailureReason(fileImport.getFailureReason());
        response.setCreatedAt(fileImport.getCreatedAt());
        response.setUpdatedAt(fileImport.getUpdatedAt());
        response.setCompletedAt(fileImport.getCompletedAt());
        return response;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "file must not be empty");
        }
        if (file.getSize() > KnowledgeFileImportParser.MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(HttpStatus.PAYLOAD_TOO_LARGE, "file size must be <= 5MB");
        }
        String extension = parser.resolveExtension(file.getOriginalFilename());
        if (!parser.isSupportedExtension(extension)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + extension);
        }
    }

    private byte[] readFileBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Failed to read uploaded file");
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType.trim();
    }

    private AppUser getCurrentUserEntity(AuthenticatedUser authenticatedUser) {
        return appUserRepository.findById(getCurrentUserId(authenticatedUser))
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Long getCurrentUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.getId() == null) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return authenticatedUser.getId();
    }

    private List<String> normalizeTags(String defaultTags) {
        if (defaultTags == null || defaultTags.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String tag : defaultTags.split(",")) {
            String value = normalize(tag);
            if (value == null || value.isBlank()) {
                continue;
            }
            if (value.length() > 50) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "default tag length must be <= 50");
            }
            normalized.add(value.toLowerCase(Locale.ROOT));
        }
        return List.copyOf(normalized);
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist file import tags");
        }
    }

    private List<String> readJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read file import tags");
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
