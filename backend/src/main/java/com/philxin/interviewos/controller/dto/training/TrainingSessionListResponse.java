package com.philxin.interviewos.controller.dto.training;

import java.util.List;

/**
 * 训练会话分页列表响应。
 */
public class TrainingSessionListResponse {
    private List<TrainingSessionSummaryResponse> items;
    private int page;
    private int size;
    private long total;
    private boolean hasNext;

    public List<TrainingSessionSummaryResponse> getItems() {
        return items;
    }

    public void setItems(List<TrainingSessionSummaryResponse> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }
}
