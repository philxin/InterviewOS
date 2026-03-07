package com.philxin.interviewos.controller.dto.knowledge;

import com.philxin.interviewos.entity.Knowledge;
import java.time.LocalDateTime;

/**
 * 知识点接口响应体。
 */
public class KnowledgeResponse {
    private Long id;
    private String title;
    private String content;
    private Integer mastery;
    private LocalDateTime createdAt;

    /**
     * 将实体转换为对外响应，避免 Controller 直接暴露实体结构。
     */
    public static KnowledgeResponse fromEntity(Knowledge knowledge) {
        KnowledgeResponse response = new KnowledgeResponse();
        response.setId(knowledge.getId());
        response.setTitle(knowledge.getTitle());
        response.setContent(knowledge.getContent());
        response.setMastery(knowledge.getMastery());
        response.setCreatedAt(knowledge.getCreatedAt());
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
