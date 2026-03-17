package com.philxin.interviewos.controller.dto.dashboard;

import java.util.List;

/**
 * 薄弱知识点摘要项。
 */
public class WeakKnowledgeItemResponse {
    private Long knowledgeId;
    private String title;
    private Integer mastery;
    private List<String> tags;

    public Long getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(Long knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
}
