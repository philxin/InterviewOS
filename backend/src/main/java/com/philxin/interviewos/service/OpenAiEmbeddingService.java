package com.philxin.interviewos.service;

import com.philxin.interviewos.config.RagProperties;
import java.util.List;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Service;

/**
 * 基于 Spring AI OpenAI 的 embedding 服务。
 */
@Service
public class OpenAiEmbeddingService implements EmbeddingService {
    private final EmbeddingModel embeddingModel;
    private final RagProperties ragProperties;

    public OpenAiEmbeddingService(EmbeddingModel embeddingModel, RagProperties ragProperties) {
        this.embeddingModel = embeddingModel;
        this.ragProperties = ragProperties;
    }

    @Override
    public float[] embed(String text) {
        return embeddingModel.call(new EmbeddingRequest(List.of(text), buildOptions())).getResult().getOutput();
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return embeddingModel.call(new EmbeddingRequest(texts, buildOptions()))
            .getResults()
            .stream()
            .map(result -> (float[]) result.getOutput())
            .toList();
    }

    @Override
    public String getModel() {
        return ragProperties.getEmbeddingModel();
    }

    @Override
    public int getDimensions() {
        return ragProperties.getEmbeddingDimensions() == null ? embeddingModel.dimensions() : ragProperties.getEmbeddingDimensions();
    }

    private OpenAiEmbeddingOptions buildOptions() {
        OpenAiEmbeddingOptions options = new OpenAiEmbeddingOptions();
        options.setModel(ragProperties.getEmbeddingModel());
        options.setDimensions(ragProperties.getEmbeddingDimensions());
        return options;
    }
}
