package com.philxin.interviewos.controller.dto.knowledge;

import com.philxin.interviewos.entity.Knowledge;
import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * 知识点接口响应体。
 */
public class KnowledgeResponse {
    private Long id;
    private String title;
    private String content;
    private Integer mastery;
    private List<String> tags;
    private String sourceType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime archivedAt;

    /**
     * 将实体转换为对外响应，避免 Controller 直接暴露实体结构。
     */
    public static KnowledgeResponse fromEntity(Knowledge knowledge) {
        KnowledgeResponse response = new KnowledgeResponse();
        response.setId(knowledge.getId());
        response.setTitle(knowledge.getTitle());
        response.setContent(knowledge.getContent());
        response.setMastery(knowledge.getMastery());
        response.setTags(
            knowledge.getTags() == null
                ? List.of()
                : knowledge.getTags().stream().map(tag -> tag.getTag()).collect(Collectors.toList())
        );
        response.setSourceType(knowledge.getSourceType() == null ? null : knowledge.getSourceType().name());
        response.setStatus(knowledge.getStatus() == null ? null : knowledge.getStatus().name());
        response.setCreatedAt(knowledge.getCreatedAt());
        response.setUpdatedAt(knowledge.getUpdatedAt());
        response.setArchivedAt(knowledge.getArchivedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getMastery() {
        return mastery;
    }

    public void setMastery(Integer mastery) {
        this.mastery = mastery;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(LocalDateTime archivedAt) {
        this.archivedAt = archivedAt;
    }
}
