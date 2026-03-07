package com.philxin.interviewos.controller.dto.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建知识点请求体。
 */
public class CreateKnowledgeRequest {
    @NotBlank(message = "title must not be blank")
    @Size(max = 200, message = "title length must be <= 200")
    private String title;

    @NotBlank(message = "content must not be blank")
    private String content;

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
}
