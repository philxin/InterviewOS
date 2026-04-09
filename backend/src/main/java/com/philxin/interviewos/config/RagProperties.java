package com.philxin.interviewos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * V3 RAG 相关配置：切片、embedding 与检索阈值。
 */
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {
    private String embeddingModel = "text-embedding-3-small";
    private Integer embeddingDimensions = 1536;
    private Integer chunkSize = 1200;
    private Integer chunkOverlap = 200;
    private Integer minChunkLength = 80;
    private Integer maxChunksPerDocument = 128;
    private Integer searchTopK = 5;
    private Double minSimilarityScore = 0.25d;

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Integer getEmbeddingDimensions() {
        return embeddingDimensions;
    }

    public void setEmbeddingDimensions(Integer embeddingDimensions) {
        this.embeddingDimensions = embeddingDimensions;
    }

    public Integer getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(Integer chunkSize) {
        this.chunkSize = chunkSize;
    }

    public Integer getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(Integer chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public Integer getMinChunkLength() {
        return minChunkLength;
    }

    public void setMinChunkLength(Integer minChunkLength) {
        this.minChunkLength = minChunkLength;
    }

    public Integer getMaxChunksPerDocument() {
        return maxChunksPerDocument;
    }

    public void setMaxChunksPerDocument(Integer maxChunksPerDocument) {
        this.maxChunksPerDocument = maxChunksPerDocument;
    }

    public Integer getSearchTopK() {
        return searchTopK;
    }

    public void setSearchTopK(Integer searchTopK) {
        this.searchTopK = searchTopK;
    }

    public Double getMinSimilarityScore() {
        return minSimilarityScore;
    }

    public void setMinSimilarityScore(Double minSimilarityScore) {
        this.minSimilarityScore = minSimilarityScore;
    }
}
