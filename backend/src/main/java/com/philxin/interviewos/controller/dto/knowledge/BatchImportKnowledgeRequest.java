package com.philxin.interviewos.controller.dto.knowledge;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量导入知识点请求体。
 */
public class BatchImportKnowledgeRequest {
    @NotEmpty(message = "items must not be empty")
    private List<BatchImportKnowledgeItemRequest> items;

    public List<BatchImportKnowledgeItemRequest> getItems() {
        return items;
    }

    public void setItems(List<BatchImportKnowledgeItemRequest> items) {
        this.items = items;
    }
}
