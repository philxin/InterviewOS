package com.philxin.interviewos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Embedding 调用独立配置，支持与对话模型使用不同的网关和密钥。
 */
@ConfigurationProperties(prefix = "app.ai.embedding")
public class EmbeddingApiProperties {
    private String baseUrl = "https://api.openai.com";
    private String apiKey = "";
    private String endpoint = "/v1/embeddings";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}

