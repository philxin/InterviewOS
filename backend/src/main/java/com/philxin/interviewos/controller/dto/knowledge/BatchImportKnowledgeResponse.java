package com.philxin.interviewos.controller.dto.knowledge;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入知识点响应体。
 */
public class BatchImportKnowledgeResponse {
    private int createdCount;
    private int failedCount;
    private List<FailedItem> failedItems = new ArrayList<>();

    public boolean allFailed() {
        return createdCount == 0 && failedCount > 0;
    }

    public int getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(int createdCount) {
        this.createdCount = createdCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<FailedItem> getFailedItems() {
        return failedItems;
    }

    public void setFailedItems(List<FailedItem> failedItems) {
        this.failedItems = failedItems;
    }

    /**
     * 单条失败项摘要。
     */
    public static class FailedItem {
        private int index;
        private String title;
        private String reason;

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
