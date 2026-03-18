package com.philxin.interviewos.controller.dto.dashboard;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 回练提醒列表响应。
 */
public class ReviewReminderResponse {
    private List<ReviewReminderItemResponse> items;
    private LocalDateTime generatedAt;

    public List<ReviewReminderItemResponse> getItems() {
        return items;
    }

    public void setItems(List<ReviewReminderItemResponse> items) {
        this.items = items;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }
}
