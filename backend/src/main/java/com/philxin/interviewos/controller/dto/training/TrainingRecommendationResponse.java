package com.philxin.interviewos.controller.dto.training;

import java.util.List;

/**
 * 今日推荐训练包响应。
 */
public class TrainingRecommendationResponse {
    private String packageId;
    private String title;
    private List<TrainingRecommendationItemResponse> items;

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<TrainingRecommendationItemResponse> getItems() {
        return items;
    }

    public void setItems(List<TrainingRecommendationItemResponse> items) {
        this.items = items;
    }
}
