package com.philxin.interviewos.controller.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建知识点请求体。
 */
public class CreateKnowledgeRequest {
    @NotBlank(message = "title must not be blank")
    @Size(max = 200, message = "title length must be <= 200")
    private String title;

    @NotBlank(message = "content must not be blank")
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
}
