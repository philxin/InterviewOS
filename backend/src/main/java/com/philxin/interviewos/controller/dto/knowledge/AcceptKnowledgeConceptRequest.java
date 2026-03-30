package com.philxin.interviewos.controller.dto.knowledge;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import java.util.List;

/**
 * 接受候选知识点请求体。
 */
public class AcceptKnowledgeConceptRequest {
    @Min(value = 1, message = "mergeKnowledgeId must be >= 1")
    private Long mergeKnowledgeId;

    @Size(max = 200, message = "title length must be <= 200")
    private String title;

    private String content;

    private List<@Size(max = 50, message = "tag length must be <= 50") String> tags;

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

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Long getMergeKnowledgeId() {
        return mergeKnowledgeId;
    }

    public void setMergeKnowledgeId(Long mergeKnowledgeId) {
        this.mergeKnowledgeId = mergeKnowledgeId;
    }
}
