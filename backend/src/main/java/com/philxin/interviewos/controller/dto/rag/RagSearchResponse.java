package com.philxin.interviewos.controller.dto.rag;

import java.util.List;
import java.util.UUID;

/**
 * RAG 搜索响应。
 */
public class RagSearchResponse {
    private String query;
    private UUID documentId;
    private int topK;
    private int hitCount;
    private boolean degraded;
    private List<RagSearchItemResponse> items;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public List<RagSearchItemResponse> getItems() {
        return items;
    }

    public void setItems(List<RagSearchItemResponse> items) {
        this.items = items;
    }
}
