package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.config.EmbeddingApiProperties;
import com.philxin.interviewos.config.RagProperties;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 基于 OpenAI 兼容接口的 embedding 服务，支持与 chat 独立的网关与密钥。
 */
@Service
public class OpenAiEmbeddingService implements EmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

    private final RestClient restClient;
    private final RagProperties ragProperties;
    private final EmbeddingApiProperties embeddingApiProperties;

    public OpenAiEmbeddingService(
        RestClient.Builder restClientBuilder,
        RagProperties ragProperties,
        EmbeddingApiProperties embeddingApiProperties
    ) {
        this.restClient = restClientBuilder.build();
        this.ragProperties = ragProperties;
        this.embeddingApiProperties = embeddingApiProperties;
    }

    @Override
    public float[] embed(String text) {
        List<float[]> embeddings = embed(List.of(text));
        if (embeddings.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Embedding provider returned empty result");
        }
        return embeddings.get(0);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        String apiKey = normalize(embeddingApiProperties.getApiKey());
        if (apiKey.isEmpty()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Embedding API key is not configured");
        }

        OpenAiEmbeddingRequest request = new OpenAiEmbeddingRequest(
            ragProperties.getEmbeddingModel(),
            texts,
            ragProperties.getEmbeddingDimensions()
        );
        OpenAiEmbeddingResponse response;
        try {
            response = restClient.post()
                .uri(resolveEmbeddingUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(OpenAiEmbeddingResponse.class);
        } catch (RestClientException exception) {
            log.warn("Embedding provider call failed: {}", exception.getMessage());
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Failed to call embedding provider");
        }

        if (response == null) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Embedding provider returned empty response");
        }
        if (response.error() != null && !normalize(response.error().message()).isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Embedding provider error: " + response.error().message());
        }
        if (response.data() == null || response.data().isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "Embedding provider returned empty result");
        }

        return response.data().stream()
            .sorted(Comparator.comparingInt(item -> item.index() == null ? Integer.MAX_VALUE : item.index()))
            .map(item -> toFloatArray(item.embedding()))
            .filter(embedding -> embedding.length > 0)
            .toList();
    }

    @Override
    public String getModel() {
        return ragProperties.getEmbeddingModel();
    }

    @Override
    public int getDimensions() {
        return ragProperties.getEmbeddingDimensions() == null ? 1536 : ragProperties.getEmbeddingDimensions();
    }

    private String resolveEmbeddingUrl() {
        String endpoint = normalize(embeddingApiProperties.getEndpoint());
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        String baseUrl = normalize(embeddingApiProperties.getBaseUrl());
        if (baseUrl.isEmpty()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "Embedding base URL is not configured");
        }
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return normalizedBaseUrl + normalizedEndpoint;
    }

    private float[] toFloatArray(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new float[0];
        }
        float[] embedding = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Double value = values.get(i);
            embedding[i] = value == null ? 0f : value.floatValue();
        }
        return embedding;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record OpenAiEmbeddingRequest(String model, List<String> input, Integer dimensions) {
    }

    private record OpenAiEmbeddingResponse(List<OpenAiEmbeddingData> data, OpenAiErrorResponse error) {
    }

    private record OpenAiEmbeddingData(Integer index, List<Double> embedding) {
    }

    private record OpenAiErrorResponse(String message) {
    }
}
